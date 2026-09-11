package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;

/**
 * Core day-by-day backtest engine for the rotational ORB strategy on intraday bars.
 *
 * <p>All strategy logic is driven by the {@link RotationalBacktestConfig} — the engine
 * builds its filters, exit rules, entry condition, and ranker from config data.</p>
 *
 * <p>For each trading day:</p>
 * <ol>
 *   <li>For each scrip in the universe, run the {@link OrbSimulator} to determine if
 *       a breakout occurred, the entry side, and entry/exit prices.</li>
 *   <li>If a {@link FeatureObserver} is registered, pass the day's data to it for
 *       feature computation and logging.</li>
 *   <li>Rank all triggered scrips and pick the top N using the configured ranker.</li>
 *   <li>Size positions — all capital allocated to the configured side.</li>
 *   <li>Record trades in the {@link PortfolioTracker}.</li>
 * </ol>
 */
@Slf4j
public final class RotationalBacktestEngine {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final RotationalBacktestConfig config;

    public RotationalBacktestEngine(RotationalBacktestConfig config) {
        this.config = config;
    }

    /**
     * Observer for feature computation. Called once per trading day with the
     * triggered ORB results. Implementations can compute and log features
     * for analysis without affecting the backtest itself.
     */
    @FunctionalInterface
    public interface FeatureObserver {
        void observe(LocalDate date, Map<String, OrbSimulator.OrbResult> orbResults,
                     Map<String, DaySlice> daySlices);
    }

    /**
     * Enriched ranker that receives full bar data and daily indicators
     * to compute scores involving features like gap, RVOL and RS rank.
     */
    public interface EnrichedRanker {

        void init(Map<String, Bars> intradayBars,
                  Map<String, Bars> dailyBars,
                  int orBarCount);

        List<RankedPick> rank(LocalDate date,
                              Map<String, OrbSimulator.OrbResult> orbResults,
                              Map<String, DaySlice> daySlices,
                              Map<String, Bars> intradayBars,
                              Map<String, Bars> dailyBars,
                              Map<String, float[]> dailyAtrMap,
                              Map<String, Map<LocalDate, Integer>> dailyDateIdx,
                              int picks);
    }

    /**
     * A ranked pick returned by {@link EnrichedRanker}.
     */
    public record RankedPick(String symbol, RotationalTrade.Side side, double score) {}

    /** A day's bar range for a single scrip. */
    public record DaySlice(int startIdx, int barCount) {}

    /**
     * Run the backtest using the config's ranker type.
     *
     * @param intradayBars    per-scrip intraday bars, keyed by scripId
     * @param dailyBars       per-scrip daily bars (null = no ATR-based features)
     * @param featureObserver optional observer for feature computation (null = skip)
     * @return backtest results
     */
    public RotationalBacktestResult run(
            Map<String, Bars> intradayBars,
            Map<String, Bars> dailyBars,
            FeatureObserver featureObserver) {
        return run(intradayBars, dailyBars, featureObserver, null);
    }

    /**
     * Run the backtest with an externally-supplied ranker (overrides config's rankerType).
     *
     * @param intradayBars       per-scrip intraday bars, keyed by scripId
     * @param dailyBars          per-scrip daily bars (null = no ATR-based features)
     * @param featureObserver    optional observer for feature computation (null = skip)
     * @param externalRanker     if non-null, use this ranker instead of the config's rankerType
     * @return backtest results
     */
    public RotationalBacktestResult run(
            Map<String, Bars> intradayBars,
            Map<String, Bars> dailyBars,
            FeatureObserver featureObserver,
            EnrichedRanker externalRanker) {

        config.validate();

        OrbSimulator simulator = new OrbSimulator(config);
        PortfolioTracker portfolio = new PortfolioTracker(config.getInitialCapital());
        PositionSizer sizer = new PositionSizer(config.isAtrScaling());

        // Build the ranker from config, or use the externally-supplied one
        EnrichedRanker enrichedRanker = (externalRanker != null) ? externalRanker : buildRanker(config);

        // Initialize enriched ranker if present
        if (enrichedRanker != null) {
            enrichedRanker.init(intradayBars, dailyBars, simulator.getOrBarCount());
        }

        // Pre-compute daily ATR(14) arrays and date-index maps per symbol
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
        Map<String, List<DaySlice>> daySlicesBySymbol = new LinkedHashMap<>();
        TreeSet<LocalDate> allDates = new TreeSet<>();

        for (Map.Entry<String, Bars> entry : intradayBars.entrySet()) {
            List<DaySlice> slices = sliceByDay(entry.getValue());
            daySlicesBySymbol.put(entry.getKey(), slices);
            Bars bars = entry.getValue();
            for (DaySlice ds : slices) {
                LocalDate date = Instant.ofEpochMilli(bars.getTimestamp(ds.startIdx()))
                        .atZone(IST).toLocalDate();
                allDates.add(date);
            }
        }

        // Apply date range filter if configured
        if (config.getStartDate() != null) {
            allDates = new TreeSet<>(allDates.tailSet(config.getStartDate(), true));
        }
        if (config.getEndDate() != null) {
            allDates = new TreeSet<>(allDates.headSet(config.getEndDate(), true));
        }

        // Build date -> DaySlice lookup for each symbol
        Map<String, Map<LocalDate, DaySlice>> symbolDateSlices = new LinkedHashMap<>();
        for (Map.Entry<String, List<DaySlice>> entry : daySlicesBySymbol.entrySet()) {
            String symbol = entry.getKey();
            Bars bars = intradayBars.get(symbol);
            Map<LocalDate, DaySlice> dateMap = new LinkedHashMap<>();
            for (DaySlice ds : entry.getValue()) {
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

            // Run ORB simulation for all symbols on this day
            Map<String, OrbSimulator.OrbResult> orbResults = new LinkedHashMap<>();
            Map<String, DaySlice> daySlices = new LinkedHashMap<>();

            for (String symbol : intradayBars.keySet()) {
                Map<LocalDate, DaySlice> dateMap = symbolDateSlices.get(symbol);
                DaySlice ds = dateMap != null ? dateMap.get(date) : null;
                if (ds == null) continue;

                daySlices.put(symbol, ds);
                Bars bars = intradayBars.get(symbol);

                // Look up prior-day ATR and prior-day close
                float priorDayAtr = Float.NaN;
                float priorClose = Float.NaN;
                float[] atrArr = dailyAtrMap.get(symbol);
                Map<LocalDate, Integer> dateIdx = dailyDateIndexMap.get(symbol);
                if (atrArr != null && dateIdx != null) {
                    Integer dayIdx = dateIdx.get(date);
                    if (dayIdx != null && dayIdx > 0) {
                        priorDayAtr = atrArr[dayIdx - 1];
                        Bars db = dailyBars != null ? dailyBars.get(symbol) : null;
                        if (db != null) {
                            priorClose = db.getClose(dayIdx - 1);
                        }
                    }
                }

                // TODO: compute orbRvol and rsRank here for entry filters
                // For now, pass NaN — these are only needed when the config has
                // RVOL/RS filters enabled. The ranker computes its own values.
                float orbRvol = Float.NaN;
                float rsRank = Float.NaN;

                OrbSimulator.OrbResult result = simulator.simulateSingle(
                        bars, ds.startIdx(), ds.barCount(),
                        symbol, priorDayAtr, priorClose, orbRvol, rsRank);
                if (result != null) {
                    orbResults.put(symbol, result);
                }
            }

            // Gap-direction post-filter: remove breakouts where gap direction
            // doesn't match the required alignment with breakout direction.
            if (config.getGapDirectionMode() != RotationalBacktestConfig.GapDirectionMode.ANY
                    && dailyBars != null) {

                {
                    boolean aligned = config.getGapDirectionMode()
                            == RotationalBacktestConfig.GapDirectionMode.ALIGNED;
                    orbResults.entrySet().removeIf(entry -> {
                        String symbol = entry.getKey();
                        OrbSimulator.OrbResult orb = entry.getValue();
                        // Compute gap direction from daily bars
                        Map<LocalDate, Integer> dateIdx = dailyDateIndexMap.get(symbol);
                        if (dateIdx == null) return true; // no daily data, skip
                        Integer dayIdx = dateIdx.get(date);
                        if (dayIdx == null || dayIdx < 1) return true;
                        Bars db = dailyBars.get(symbol);
                        if (db == null) return true;
                        float priorClose = db.getClose(dayIdx - 1);
                        float todayOpen = db.getOpen(dayIdx);
                        if (priorClose <= 0) return true;
                        boolean gapUp = todayOpen > priorClose;
                        boolean isLong = orb.side() == RotationalTrade.Side.LONG;
                        // ALIGNED: long+gapUp or short+gapDown
                        // OPPOSITE: long+gapDown or short+gapUp
                        boolean keep = aligned ? (isLong == gapUp) : (isLong != gapUp);
                        return !keep;
                    });
                }
            }

            // Feature observation
            if (featureObserver != null) {
                featureObserver.observe(date, orbResults, daySlices);
            }

            if (orbResults.isEmpty()) {
                noBreakoutDays++;
                portfolio.recordDay(dateEpoch, List.of());
                continue;
            }

            // Rank and select picks — all on the configured side
            List<String> selectedPicks;

            if (enrichedRanker != null) {
                List<RankedPick> rankedPicks = enrichedRanker.rank(date, orbResults, daySlices,
                        intradayBars, dailyBars, dailyAtrMap, dailyDateIndexMap,
                        config.getPicks());
                selectedPicks = rankedPicks.stream().map(RankedPick::symbol).toList();
            } else {
                // No ranker (NONE): trade all breakouts
                selectedPicks = new ArrayList<>(orbResults.keySet());
            }

            if (selectedPicks.isEmpty()) {
                portfolio.recordDay(dateEpoch, List.of());
                continue;
            }

            double totalCapital = portfolio.getEquity();
            boolean isLong = config.getSide() == RotationalBacktestConfig.Side.LONG;

            // Build trades — all capital to the single configured side
            List<RotationalTrade> dayTrades = new ArrayList<>();

            for (String symbol : selectedPicks) {
                OrbSimulator.OrbResult r = orbResults.get(symbol);
                if (r == null) continue;
                double perStock = totalCapital / Math.max(1, selectedPicks.size());
                double shares = Math.floor(perStock / r.entryPrice());
                if (shares <= 0) continue;

                double notional = shares * r.entryPrice();
                double grossPnl = isLong
                        ? shares * (r.exitPrice() - r.entryPrice())
                        : shares * (r.entryPrice() - r.exitPrice());
                double netPnl = grossPnl - notional * config.getSlippage();

                dayTrades.add(new RotationalTrade(dateEpoch, symbol,
                        r.side(),
                        r.entryPrice(), r.exitPrice(), shares,
                        grossPnl, netPnl, 0,
                        r.entryTime(), r.orHigh(), r.orLow(), r.exitReason()));
            }

            allTrades.addAll(dayTrades);
            portfolio.recordDay(dateEpoch, dayTrades);

            if (dayNumber % 50 == 0) {
                log.info("Day {}: {} picks ({}), {} trades, equity={}",
                        dayNumber, selectedPicks.size(), config.getSide(),
                        dayTrades.size(), String.format("%.0f", portfolio.getEquity()));
            }
        }

        RotationalMetrics metrics = new RotationalMetrics(allTrades, portfolio,
                config.getInitialCapital());

        log.info("Backtest complete — {} trading days, {} trades", allDates.size(), allTrades.size());
        log.info("No-breakout days: {}", noBreakoutDays);

        return new RotationalBacktestResult(allTrades, portfolio, metrics);
    }

    // ── Ranker construction ──────────────────────────────────────────────

    /**
     * Build the ranker from the config's RankerType.
     * Returns null for NONE (trade all breakouts).
     */
    private static EnrichedRanker buildRanker(RotationalBacktestConfig config) {
        return switch (config.getRankerType()) {
            case RS       -> new ConfigurableRanker(false, false, true);
            case RVOL     -> new ConfigurableRanker(false, true, false);
            case GAP      -> new ConfigurableRanker(true, false, false);
            case RS_RVOL  -> new ConfigurableRanker(false, true, true);
            case RS_GAP   -> new ConfigurableRanker(true, false, true);
            case RVOL_GAP -> new ConfigurableRanker(true, true, false);
            case GAP_RVOL_RS -> new ConfigurableRanker(true, true, true);
            case RANDOM -> new RandomRanker();
            case ALPHABETICAL -> new AlphabeticalRanker();
            case NONE -> null;
        };
    }

    /**
     * Simple random ranker for baseline comparison.
     */
    private static final class RandomRanker implements EnrichedRanker {
        private final Random random = new Random(42);

        @Override
        public void init(Map<String, Bars> intradayBars, Map<String, Bars> dailyBars, int orBarCount) {}

        @Override
        public List<RankedPick> rank(LocalDate date,
                                     Map<String, OrbSimulator.OrbResult> orbResults,
                                     Map<String, DaySlice> daySlices,
                                     Map<String, Bars> intradayBars,
                                     Map<String, Bars> dailyBars,
                                     Map<String, float[]> dailyAtrMap,
                                     Map<String, Map<LocalDate, Integer>> dailyDateIdx,
                                     int picks) {
            List<String> candidates = new ArrayList<>(orbResults.keySet());
            Collections.shuffle(candidates, random);

            List<RankedPick> result = new ArrayList<>();
            for (int i = 0; i < Math.min(picks, candidates.size()); i++) {
                OrbSimulator.OrbResult r = orbResults.get(candidates.get(i));
                result.add(new RankedPick(candidates.get(i), r.side(), 0));
            }
            return result;
        }
    }

    /**
     * Deterministic alphabetical ranker — sorts candidates by symbol name.
     * Provides a stable, reproducible baseline that doesn't depend on any computed features.
     */
    private static final class AlphabeticalRanker implements EnrichedRanker {

        @Override
        public void init(Map<String, Bars> intradayBars, Map<String, Bars> dailyBars, int orBarCount) {}

        @Override
        public List<RankedPick> rank(LocalDate date,
                                     Map<String, OrbSimulator.OrbResult> orbResults,
                                     Map<String, DaySlice> daySlices,
                                     Map<String, Bars> intradayBars,
                                     Map<String, Bars> dailyBars,
                                     Map<String, float[]> dailyAtrMap,
                                     Map<String, Map<LocalDate, Integer>> dailyDateIdx,
                                     int picks) {
            List<String> candidates = new ArrayList<>(orbResults.keySet());
            Collections.sort(candidates);

            List<RankedPick> result = new ArrayList<>();
            for (int i = 0; i < Math.min(picks, candidates.size()); i++) {
                OrbSimulator.OrbResult r = orbResults.get(candidates.get(i));
                result.add(new RankedPick(candidates.get(i), r.side(), 0));
            }
            return result;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Slice a flat array of intraday bars into per-day segments.
     */
    static List<DaySlice> sliceByDay(Bars bars) {
        List<DaySlice> days = new ArrayList<>();
        if (bars.size() == 0) return days;

        int dayStart = 0;
        LocalDate currentDate = Instant.ofEpochMilli(bars.getTimestamp(0))
                .atZone(IST).toLocalDate();

        for (int i = 1; i < bars.size(); i++) {
            LocalDate barDate = Instant.ofEpochMilli(bars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            if (!barDate.equals(currentDate)) {
                days.add(new DaySlice(dayStart, i - dayStart));
                dayStart = i;
                currentDate = barDate;
            }
        }
        days.add(new DaySlice(dayStart, bars.size() - dayStart));
        return days;
    }

}
