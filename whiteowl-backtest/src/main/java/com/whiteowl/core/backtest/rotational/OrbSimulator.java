package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates ORB (Opening Range Breakout) trades on intraday bars.
 *
 * <p>For each trading day and scrip:</p>
 * <ol>
 *   <li>Compute the Opening Range (OR high/low) from the first N minutes of bars.</li>
 *   <li>Compute OR-derived features: IBS.</li>
 *   <li>Check entry filters (gap/ATR, OR/ATR, RVOL, RS rank, IBS).</li>
 *   <li>Scan subsequent bars for an entry trigger using the configured entry method.</li>
 *   <li>Respect entry cutoff time — no new entries after the cutoff.</li>
 *   <li>After entry, evaluate exit rules bar-by-bar in priority order.</li>
 *   <li>If re-entries are allowed, continue scanning after exit for additional entries.</li>
 * </ol>
 *
 * <p>The simulator builds its entry condition, filters, and exit rules from the
 * {@link RotationalBacktestConfig} — all strategy logic is data-driven.</p>
 */
public final class OrbSimulator {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final RotationalBacktestConfig config;
    private final int orBarCount;
    private final EntryCondition entryCondition;
    private final List<EntryFilter> entryFilters;

    public OrbSimulator(RotationalBacktestConfig config) {
        this.config = config;
        this.orBarCount = Math.max(1, config.getOpeningRangeMinutes() / config.getBarMinutes());
        this.entryCondition = buildEntryCondition(config);
        this.entryFilters = buildEntryFilters(config);
    }

    /**
     * Result of an ORB simulation for one scrip on one day.
     * Null if no trade was triggered (no breakout, not enough data, cutoff, etc.).
     */
    public record OrbResult(RotationalTrade.Side side, double entryPrice, double exitPrice,
                            LocalTime entryTime, float orHigh, float orLow,
                            ExitReason exitReason) {}

    /**
     * Simulate the ORB for a single scrip on a single day.
     * Returns a list of results (multiple results if re-entries are allowed).
     *
     * @param bars       the scrip's intraday bars (all bars, not just this day)
     * @param dayStart   index of the first bar of this day in the bars array
     * @param dayCount   number of bars in this day
     * @param symbol     the scrip ID (for entry filter context)
     * @param dailyAtr   prior-day ATR(14) for this symbol, or Float.NaN if unavailable
     * @param priorClose prior day's closing price, or Float.NaN if unavailable
     * @param orbRvol    relative volume during OR, or Float.NaN if unavailable
     * @param rsRank     cross-sectional RS rank (0-100), or Float.NaN if unavailable
     * @return list of OrbResults (empty if no trade triggered)
     */
    public List<OrbResult> simulate(Bars bars, int dayStart, int dayCount,
                                    String symbol, float dailyAtr, float priorClose,
                                    float orbRvol, float rsRank) {
        List<OrbResult> results = new ArrayList<>();

        if (dayCount < orBarCount + 1) return results;

        // Compute Opening Range
        float orHigh = Float.MIN_VALUE;
        float orLow = Float.MAX_VALUE;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            orHigh = Math.max(orHigh, bars.getHigh(i));
            orLow = Math.min(orLow, bars.getLow(i));
        }

        // Compute IBS of opening range
        float orRange = orHigh - orLow;
        float orbIbs = Float.NaN;
        float orClose = bars.getClose(dayStart + orBarCount - 1);
        if (orRange > 0) {
            orbIbs = (orClose - orLow) / orRange;
        }

        // Compute OR body % and spread % for side-specific filtering
        float orOpen = bars.getOpen(dayStart);
        double orBodyPct = Double.NaN;
        double orSpreadPct = Double.NaN;
        if (orClose > 0) {
            orBodyPct = Math.abs(orClose - orOpen) / orClose * 100.0;
            orSpreadPct = (orHigh - orLow) / orClose * 100.0;
        }

        // Compute gap/ATR for side-specific filtering in scanForEntry
        float todayOpen = bars.getOpen(dayStart);
        double gapAtr = Double.NaN;
        if (!Float.isNaN(dailyAtr) && dailyAtr > 0 && !Float.isNaN(todayOpen)
                && !Float.isNaN(priorClose) && priorClose > 0) {
            gapAtr = (todayOpen - priorClose) / dailyAtr;
        }

        // Check entry filters
        if (!entryFilters.isEmpty()) {
            var filterCtx = new EntryFilter.EntryFilterContext(
                    symbol, orHigh, orLow, dailyAtr, todayOpen, priorClose,
                    orbIbs, orbRvol, rsRank);
            for (EntryFilter filter : entryFilters) {
                if (!filter.accept(filterCtx)) return results;
            }
        }

        // Scan for entries (with re-entry support)
        int scanStart = dayStart + orBarCount;
        int scanEnd = dayStart + dayCount;
        int maxTrades = 1 + config.getMaxReEntries();

        // Side is always fixed — a single strategy trades exactly one side.
        RotationalTrade.Side requiredSide = config.getSide() == RotationalBacktestConfig.Side.LONG
                ? RotationalTrade.Side.LONG : RotationalTrade.Side.SHORT;
        RotationalTrade.Side lastSide = requiredSide;

        for (int tradeNum = 0; tradeNum < maxTrades; tradeNum++) {
            OrbResult result = scanForEntry(bars, scanStart, scanEnd, orHigh, orLow,
                    dailyAtr, lastSide, gapAtr, orBodyPct, orSpreadPct, orbRvol, rsRank);
            if (result == null) break;

            results.add(result);
            lastSide = result.side();

            // For re-entries: continue scanning from after the exit bar
            // Find the bar where the exit occurred
            scanStart = findExitBarIndex(bars, scanStart, scanEnd, result);
            if (scanStart >= scanEnd) break;
        }

        return results;
    }

    /**
     * Backward-compatible simulate: returns the first OrbResult or null.
     */
    public OrbResult simulateSingle(Bars bars, int dayStart, int dayCount,
                                    String symbol, float dailyAtr, float priorClose,
                                    float orbRvol, float rsRank) {
        List<OrbResult> results = simulate(bars, dayStart, dayCount, symbol,
                dailyAtr, priorClose, orbRvol, rsRank);
        return results.isEmpty() ? null : results.getFirst();
    }

    /**
     * Scan for a single entry starting from scanStart.
     */
    private OrbResult scanForEntry(Bars bars, int scanStart, int scanEnd,
                                   float orHigh, float orLow, float dailyAtr,
                                   RotationalTrade.Side requiredSide, double gapAtr,
                                   double orBodyPct, double orSpreadPct,
                                   float orbRvol, float rsRank) {
        double marketClose = bars.getClose(scanEnd - 1);

        for (int i = scanStart; i < scanEnd; i++) {
            LocalTime barTime = Instant.ofEpochMilli(bars.getTimestamp(i))
                    .atZone(IST).toLocalTime();

            // Respect entry cutoff
            if (!barTime.isBefore(config.getEntryCutoffTime())) break;

            EntryCondition.EntrySignal entry = entryCondition.check(bars, i, orHigh, orLow);
            if (entry != null) {
                // Side restriction: skip entries on the disallowed side.
                // For allowedSide=LONG_ONLY/SHORT_ONLY this ensures the simulator
                // never generates breakouts on the wrong side.
                // For re-entries this ensures same-direction re-entry.
                if (requiredSide != null && entry.side() != requiredSide) {
                    // Edge case (BREAKOUT mode): a bar may break both OR-high and
                    // OR-low when its range exceeds the opening range. The entry
                    // condition returns the first side it checks (LONG), but the
                    // required side may be SHORT. Try the required side directly.
                    if (config.getEntryMethod() == RotationalBacktestConfig.EntryMethod.BREAKOUT) {
                        if (requiredSide == RotationalTrade.Side.LONG
                                && bars.getHigh(i) > orHigh) {
                            entry = new EntryCondition.EntrySignal(
                                    RotationalTrade.Side.LONG, orHigh);
                        } else if (requiredSide == RotationalTrade.Side.SHORT
                                && bars.getLow(i) < orLow) {
                            entry = new EntryCondition.EntrySignal(
                                    RotationalTrade.Side.SHORT, orLow);
                        } else {
                            continue;
                        }
                    } else {
                        // CANDLE_CLOSE: close can't be on both sides simultaneously
                        continue;
                    }
                }

                // OR body filter (0 = disabled)
                if (!Double.isNaN(orBodyPct)) {
                    if (config.getMinOrBodyPct() > 0 && orBodyPct < config.getMinOrBodyPct()) continue;
                    if (config.getMaxOrBodyPct() > 0 && orBodyPct > config.getMaxOrBodyPct()) continue;
                }
                // OR spread filter (0 = disabled)
                if (!Double.isNaN(orSpreadPct)) {
                    if (config.getMinOrSpreadPct() > 0 && orSpreadPct < config.getMinOrSpreadPct()) continue;
                    if (config.getMaxOrSpreadPct() > 0 && orSpreadPct > config.getMaxOrSpreadPct()) continue;
                }

                return resolveExit(bars, i, scanEnd, entry.side(),
                        entry.entryPrice(), marketClose, barTime, orHigh, orLow, dailyAtr);
            }
        }
        return null;
    }

    /**
     * After an entry is triggered, evaluate exit rules bar-by-bar.
     */
    private OrbResult resolveExit(Bars bars, int entryBarIdx, int scanEnd,
                                   RotationalTrade.Side side, double entryPrice,
                                   double marketClose, LocalTime entryTime,
                                   float orHigh, float orLow, float dailyAtr) {

        List<ExitRule> exitRules = buildExitRules(config);

        ExitRule.ExitContext ctx = new ExitRule.ExitContext(side, entryPrice,
                orHigh, orLow, dailyAtr);

        // Initialize all exit rules
        for (ExitRule rule : exitRules) {
            rule.init(ctx);
        }

        // Scan from the bar AFTER entry through end of day
        for (int j = entryBarIdx + 1; j < scanEnd; j++) {
            for (ExitRule rule : exitRules) {
                ExitRule.ExitSignal signal = rule.check(bars, j, ctx);
                if (signal != null) {
                    return new OrbResult(side, entryPrice, signal.exitPrice(),
                            entryTime, orHigh, orLow, signal.reason());
                }
            }
        }

        // No exit rule fired — close at market close
        return new OrbResult(side, entryPrice, marketClose, entryTime,
                orHigh, orLow, ExitReason.MARKET_CLOSE);
    }

    /**
     * Find the bar index at which the exit occurred (for re-entry scanning).
     */
    private int findExitBarIndex(Bars bars, int scanStart, int scanEnd,
                                 OrbResult result) {
        // For time-based exits and market close, find by time
        // For price-based exits, find the bar where price hit the exit level
        for (int i = scanStart; i < scanEnd; i++) {
            if (result.exitReason() == ExitReason.STOP_LOSS ||
                    result.exitReason() == ExitReason.TRAILING_STOP) {
                if (result.side() == RotationalTrade.Side.LONG) {
                    if (bars.getLow(i) <= result.exitPrice()) return i + 1;
                } else {
                    if (bars.getHigh(i) >= result.exitPrice()) return i + 1;
                }
            } else if (result.exitReason() == ExitReason.TARGET) {
                if (result.side() == RotationalTrade.Side.LONG) {
                    if (bars.getHigh(i) >= result.exitPrice()) return i + 1;
                } else {
                    if (bars.getLow(i) <= result.exitPrice()) return i + 1;
                }
            } else {
                // MARKET_CLOSE or TIME_EXIT — no re-entry possible after these
                return scanEnd;
            }
        }
        return scanEnd;
    }

    /**
     * Build the entry condition from the config's EntryMethod enum.
     */
    private static EntryCondition buildEntryCondition(RotationalBacktestConfig config) {
        return switch (config.getEntryMethod()) {
            case BREAKOUT -> new BreakoutEntry();
            case CANDLE_CLOSE -> new CandleCloseEntry();
        };
    }

    /**
     * Build the list of entry filters from the config's threshold values.
     * Null thresholds are skipped (disabled).
     */
    private static List<EntryFilter> buildEntryFilters(RotationalBacktestConfig config) {
        List<EntryFilter> filters = new ArrayList<>();

        if (config.getMinGapAtr() != null || config.getMaxGapAtr() != null) {
            filters.add(ctx -> {
                double gapAtr = ctx.gapAtr();
                if (Double.isNaN(gapAtr)) return false;
                if (config.getMinGapAtr() != null && gapAtr < config.getMinGapAtr()) return false;
                if (config.getMaxGapAtr() != null && gapAtr > config.getMaxGapAtr()) return false;
                return true;
            });
        }

        if (config.getMinOrAtr() != null || config.getMaxOrAtr() != null) {
            filters.add(ctx -> {
                double orAtr = ctx.orRangeAtr();
                if (Double.isNaN(orAtr)) return false;
                if (config.getMinOrAtr() != null && orAtr < config.getMinOrAtr()) return false;
                if (config.getMaxOrAtr() != null && orAtr > config.getMaxOrAtr()) return false;
                return true;
            });
        }

        if (config.getMinOrbRvol() != null || config.getMaxOrbRvol() != null) {
            filters.add(new OrbRvolFilter(config.getMinOrbRvol(), config.getMaxOrbRvol()));
        }

        if (config.getMinRsRank() != null || config.getMaxRsRank() != null) {
            filters.add(new RsRankFilter(config.getMinRsRank(), config.getMaxRsRank()));
        }

        if (config.getMinOrbIbs() != null || config.getMaxOrbIbs() != null) {
            filters.add(new OrbIbsFilter(config.getMinOrbIbs(), config.getMaxOrbIbs()));
        }

        return filters;
    }

    /**
     * Build the exit rule chain from the config.
     * A fresh list is built for each trade because exit rules hold mutable state.
     */
    private static List<ExitRule> buildExitRules(RotationalBacktestConfig config) {
        List<ExitRule> rules = new ArrayList<>();

        // Static stop loss (always present)
        ConfigurableStopLossExit stopExit = new ConfigurableStopLossExit(
                config.getStopBasis(), config.getStopMultiplier());
        rules.add(stopExit);

        // Trailing stop (optional)
        if (config.isTrailingStopEnabled()) {
            rules.add(new TrailingStopExit(
                    config.getTrailingStopBasis(), config.getTrailingStopMultiplier()));
        }

        // Target (optional)
        if (config.isTargetEnabled()) {
            rules.add(new ConfigurableTargetExit(
                    config.getTargetBasis(), config.getTargetMultiplier(), stopExit));
        }

        // Timed exit (optional)
        if (config.getExitTime() != null) {
            rules.add(new TimedExit(config.getExitTime()));
        }

        return rules;
    }

    /**
     * Compute opening range high for a day (useful for feature computation).
     */
    public float computeOrHigh(Bars bars, int dayStart, int dayCount) {
        if (dayCount < orBarCount) return Float.NaN;
        float orHigh = Float.MIN_VALUE;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            orHigh = Math.max(orHigh, bars.getHigh(i));
        }
        return orHigh;
    }

    /**
     * Compute opening range low for a day (useful for feature computation).
     */
    public float computeOrLow(Bars bars, int dayStart, int dayCount) {
        if (dayCount < orBarCount) return Float.NaN;
        float orLow = Float.MAX_VALUE;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            orLow = Math.min(orLow, bars.getLow(i));
        }
        return orLow;
    }

    public int getOrBarCount() {
        return orBarCount;
    }
}
