package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;

/**
 * Backtest engine for a {@link CompositeOrbStrategy} — runs multiple
 * sub-strategies on shared bar data, merges breakouts, ranks with a
 * shared ranker, and allocates capital via a pluggable {@link CapitalAllocator}.
 *
 * <p>Each sub-strategy has its own {@link OrbSimulator} with independent
 * stop/target/filter settings, but the ranker and capital allocation
 * operate across all sub-strategies simultaneously.</p>
 *
 * <h3>Capital allocation</h3>
 * <p>The strategy's {@link CapitalAllocator} determines per-sub-strategy
 * capital fractions each day. Each sub-strategy's allocated capital is
 * divided equally among its actual picks that day.</p>
 */
@Slf4j
public final class CompositeBacktestEngine {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final CompositeOrbStrategy strategy;

    public CompositeBacktestEngine(CompositeOrbStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Run the composite backtest using the strategy's {@link CapitalAllocator}.
     *
     * @param intradayBars per-scrip intraday bars
     * @param dailyBars    per-scrip daily bars
     * @param ranker       shared ranker for all sub-strategies (null = trade all)
     * @return backtest results
     */
    public RotationalBacktestResult run(
            Map<String, Bars> intradayBars,
            Map<String, Bars> dailyBars,
            RotationalBacktestEngine.EnrichedRanker ranker) {

        PortfolioTracker portfolio = new PortfolioTracker(strategy.getInitialCapital());

        // Build per-sub-strategy simulators
        List<SubStrategyContext> contexts = new ArrayList<>();
        for (CompositeOrbStrategy.SubStrategy sub : strategy.getSubStrategies()) {
            sub.config().validate();
            OrbSimulator sim = new OrbSimulator(sub.config());
            contexts.add(new SubStrategyContext(sub, sim));
        }

        // Initialize ranker (use first sub-strategy's OR bar count — they should all be the same)
        int orBarCount = contexts.getFirst().simulator.getOrBarCount();
        if (ranker != null) {
            ranker.init(intradayBars, dailyBars, orBarCount);
        }

        // Pre-compute daily ATR(14) arrays and date-index maps
        Map<String, float[]> dailyAtrMap = new HashMap<>();
        Map<String, Map<LocalDate, Integer>> dailyDateIndexMap = new HashMap<>();

        if (dailyBars != null) {
            for (Map.Entry<String, Bars> entry : dailyBars.entrySet()) {
                String symbol = entry.getKey();
                Bars bars = entry.getValue();
                int size = bars.size();
                if (size > 14) {
                    dailyAtrMap.put(symbol, IndicatorFunctions.atr(
                            bars.arrays().high(), bars.arrays().low(),
                            bars.arrays().close(), size, 14));
                    Map<LocalDate, Integer> dateIdx = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        LocalDate d = Instant.ofEpochMilli(bars.getTimestamp(i))
                                .atZone(IST).toLocalDate();
                        dateIdx.put(d, i);
                    }
                    dailyDateIndexMap.put(symbol, dateIdx);
                }
            }
        }

        // Slice all scrips into per-day segments
        Map<String, List<RotationalBacktestEngine.DaySlice>> daySlicesBySymbol = new LinkedHashMap<>();
        TreeSet<LocalDate> allDates = new TreeSet<>();

        for (Map.Entry<String, Bars> entry : intradayBars.entrySet()) {
            List<RotationalBacktestEngine.DaySlice> slices = sliceByDay(entry.getValue());
            daySlicesBySymbol.put(entry.getKey(), slices);
            Bars bars = entry.getValue();
            for (RotationalBacktestEngine.DaySlice ds : slices) {
                LocalDate date = Instant.ofEpochMilli(bars.getTimestamp(ds.startIdx()))
                        .atZone(IST).toLocalDate();
                allDates.add(date);
            }
        }

        // Build date -> DaySlice lookup for each symbol
        Map<String, Map<LocalDate, RotationalBacktestEngine.DaySlice>> symbolDateSlices = new LinkedHashMap<>();
        for (Map.Entry<String, List<RotationalBacktestEngine.DaySlice>> entry : daySlicesBySymbol.entrySet()) {
            String symbol = entry.getKey();
            Bars bars = intradayBars.get(symbol);
            Map<LocalDate, RotationalBacktestEngine.DaySlice> dateMap = new LinkedHashMap<>();
            for (RotationalBacktestEngine.DaySlice ds : entry.getValue()) {
                LocalDate date = Instant.ofEpochMilli(bars.getTimestamp(ds.startIdx()))
                        .atZone(IST).toLocalDate();
                dateMap.put(date, ds);
            }
            symbolDateSlices.put(symbol, dateMap);
        }

        List<RotationalTrade> allTrades = new ArrayList<>();
        int dayNumber = 0;
        int noBreakoutDays = 0;

        for (LocalDate date : allDates) {
            dayNumber++;
            long dateEpoch = date.atStartOfDay(IST).toInstant().toEpochMilli();

            // ── 1. Run each sub-strategy's simulator ────────────────────
            // Merge all breakouts. If the same symbol triggers in multiple
            // sub-strategies, the first (by sub-strategy order) wins.
            Map<String, OrbSimulator.OrbResult> mergedOrbResults = new LinkedHashMap<>();
            Map<String, CompositeOrbStrategy.SubStrategy> breakoutSource = new HashMap<>();

            for (SubStrategyContext ctx : contexts) {
                Map<String, OrbSimulator.OrbResult> subOrbResults = new LinkedHashMap<>();

                for (Map.Entry<String, Map<LocalDate, RotationalBacktestEngine.DaySlice>> symEntry
                        : symbolDateSlices.entrySet()) {
                    String symbol = symEntry.getKey();
                    RotationalBacktestEngine.DaySlice ds = symEntry.getValue().get(date);
                    if (ds == null) continue;

                    Bars bars = intradayBars.get(symbol);
                    if (bars == null) continue;

                    // Get prior-day ATR and close
                    float priorDayAtr = Float.NaN;
                    float priorClose = Float.NaN;
                    Map<LocalDate, Integer> dateIdx = dailyDateIndexMap.get(symbol);
                    if (dateIdx != null) {
                        Integer dayIdx = dateIdx.get(date);
                        if (dayIdx != null && dayIdx >= 1) {
                            float[] atrArr = dailyAtrMap.get(symbol);
                            if (atrArr != null) priorDayAtr = atrArr[dayIdx - 1];
                            Bars db = dailyBars.get(symbol);
                            if (db != null) priorClose = db.getClose(dayIdx - 1);
                        }
                    }

                    OrbSimulator.OrbResult result = ctx.simulator.simulateSingle(
                            bars, ds.startIdx(), ds.barCount(),
                            symbol, priorDayAtr, priorClose, Float.NaN, Float.NaN);
                    if (result != null) {
                        subOrbResults.put(symbol, result);
                    }
                }

                // Apply gap-direction post-filter for this sub-strategy
                RotationalBacktestConfig cfg = ctx.subStrategy.config();
                if (cfg.getGapDirectionMode() != RotationalBacktestConfig.GapDirectionMode.ANY
                        && dailyBars != null) {
                    boolean aligned = cfg.getGapDirectionMode()
                            == RotationalBacktestConfig.GapDirectionMode.ALIGNED;
                    subOrbResults.entrySet().removeIf(entry -> {
                        String symbol = entry.getKey();
                        OrbSimulator.OrbResult orb = entry.getValue();
                        Map<LocalDate, Integer> dateIdx = dailyDateIndexMap.get(symbol);
                        if (dateIdx == null) return true;
                        Integer dayIdx = dateIdx.get(date);
                        if (dayIdx == null || dayIdx < 1) return true;
                        Bars db = dailyBars.get(symbol);
                        if (db == null) return true;
                        float pc = db.getClose(dayIdx - 1);
                        float to = db.getOpen(dayIdx);
                        if (pc <= 0) return true;
                        boolean gapUp = to > pc;
                        boolean isLong = orb.side() == RotationalTrade.Side.LONG;
                        boolean keep = aligned ? (isLong == gapUp) : (isLong != gapUp);
                        return !keep;
                    });
                }

                // Merge: first sub-strategy to claim a symbol wins
                for (Map.Entry<String, OrbSimulator.OrbResult> e : subOrbResults.entrySet()) {
                    if (!mergedOrbResults.containsKey(e.getKey())) {
                        mergedOrbResults.put(e.getKey(), e.getValue());
                        breakoutSource.put(e.getKey(), ctx.subStrategy);
                    }
                }
            }

            if (mergedOrbResults.isEmpty()) {
                noBreakoutDays++;
                portfolio.recordDay(dateEpoch, List.of());
                continue;
            }

            // ── 2. Rank merged breakouts ────────────────────────────────
            List<String> selectedPicks;

            if (ranker != null) {
                // Build daySlices for the ranker
                Map<String, RotationalBacktestEngine.DaySlice> daySlices = new HashMap<>();
                for (String symbol : mergedOrbResults.keySet()) {
                    Map<LocalDate, RotationalBacktestEngine.DaySlice> dateMap =
                            symbolDateSlices.get(symbol);
                    if (dateMap != null) {
                        RotationalBacktestEngine.DaySlice ds = dateMap.get(date);
                        if (ds != null) daySlices.put(symbol, ds);
                    }
                }

                List<RotationalBacktestEngine.RankedPick> rankedPicks = ranker.rank(
                        date, mergedOrbResults, daySlices,
                        intradayBars, dailyBars,
                        dailyAtrMap, dailyDateIndexMap,
                        strategy.getTotalPicks());

                selectedPicks = rankedPicks.stream()
                        .map(RotationalBacktestEngine.RankedPick::symbol).toList();
            } else {
                // No ranker: trade all breakouts
                selectedPicks = new ArrayList<>(mergedOrbResults.keySet());
            }

            if (selectedPicks.isEmpty()) {
                portfolio.recordDay(dateEpoch, List.of());
                continue;
            }

            double totalCapital = portfolio.getEquity();

            // ── 3. Capital allocation ────────────────────────────────────
            CapitalAllocator allocator = strategy.getAllocator();
            var allocCtx = new CapitalAllocator.AllocationContext(
                    date, totalCapital, strategy.getSubStrategies());
            Map<String, Double> allocFractions = allocator.allocate(allocCtx);

            // Group actual picks by their source sub-strategy
            Map<String, List<String>> picksByStrategy = new LinkedHashMap<>();
            for (String symbol : selectedPicks) {
                CompositeOrbStrategy.SubStrategy src = breakoutSource.get(symbol);
                if (src != null) {
                    picksByStrategy.computeIfAbsent(src.name(), k -> new ArrayList<>())
                            .add(symbol);
                }
            }

            // ── 4. Build trades ─────────────────────────────────────────
            List<RotationalTrade> dayTrades = new ArrayList<>();

            for (var stratEntry : picksByStrategy.entrySet()) {
                String stratName = stratEntry.getKey();
                List<String> symbols = stratEntry.getValue();
                double stratCapital = totalCapital
                        * allocFractions.getOrDefault(stratName, 0.0);
                double perStock = stratCapital / Math.max(1, symbols.size());

                for (String symbol : symbols) {
                    OrbSimulator.OrbResult r = mergedOrbResults.get(symbol);
                    if (r == null) continue;
                    double shares = Math.floor(perStock / r.entryPrice());
                    if (shares <= 0) continue;

                    double notional = shares * r.entryPrice();
                    double grossPnl = r.side() == RotationalTrade.Side.LONG
                            ? shares * (r.exitPrice() - r.entryPrice())
                            : shares * (r.entryPrice() - r.exitPrice());
                    double netPnl = grossPnl - notional * strategy.getSlippage();

                    dayTrades.add(new RotationalTrade(dateEpoch, symbol, r.side(),
                            r.entryPrice(), r.exitPrice(), shares,
                            grossPnl, netPnl, 0,
                            r.entryTime(), r.orHigh(), r.orLow(), r.exitReason()));
                }
            }

            allTrades.addAll(dayTrades);
            portfolio.recordDay(dateEpoch, dayTrades);

            if (dayNumber % 50 == 0) {
                log.info("Day {}: {} picks, {} trades, equity={}",
                        dayNumber, selectedPicks.size(),
                        dayTrades.size(), String.format("%.0f", portfolio.getEquity()));
            }
        }

        log.info("Backtest complete — {} trading days, {} trades",
                allDates.size(), allTrades.size());
        log.info("No-breakout days: {}", noBreakoutDays);

        RotationalMetrics metrics = new RotationalMetrics(allTrades, portfolio,
                strategy.getInitialCapital());
        return new RotationalBacktestResult(allTrades, portfolio, metrics);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private record SubStrategyContext(
            CompositeOrbStrategy.SubStrategy subStrategy,
            OrbSimulator simulator
    ) {}

    private static List<RotationalBacktestEngine.DaySlice> sliceByDay(Bars bars) {
        List<RotationalBacktestEngine.DaySlice> days = new ArrayList<>();
        if (bars.size() == 0) return days;

        int dayStart = 0;
        LocalDate currentDate = Instant.ofEpochMilli(bars.getTimestamp(0))
                .atZone(IST).toLocalDate();

        for (int i = 1; i < bars.size(); i++) {
            LocalDate barDate = Instant.ofEpochMilli(bars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            if (!barDate.equals(currentDate)) {
                days.add(new RotationalBacktestEngine.DaySlice(dayStart, i - dayStart));
                dayStart = i;
                currentDate = barDate;
            }
        }
        days.add(new RotationalBacktestEngine.DaySlice(dayStart, bars.size() - dayStart));
        return days;
    }
}
