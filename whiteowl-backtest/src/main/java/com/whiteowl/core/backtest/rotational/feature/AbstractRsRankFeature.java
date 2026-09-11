package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

import java.time.*;
import java.util.*;

/**
 * Base class for cross-sectional RS rank features.
 *
 * <p>Subclasses define how to compute each scrip's return on a given day
 * (via {@link #computeReturn}). This base class handles:</p>
 * <ul>
 *   <li>Slicing intraday bars into per-day segments</li>
 *   <li>Gathering returns across all scrips for each trading day</li>
 *   <li>Computing percentile ranks (0-100)</li>
 *   <li>Building the lookup table for per-trade access</li>
 * </ul>
 */
abstract sealed class AbstractRsRankFeature implements CrossSectionalFeature
        permits RsRankCloseToOrFeature, RsRankOrOnlyFeature {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Pre-computed lookup: "date|symbol" -> percentile rank (0-100)
    private final Map<String, Double> rankLookup = new HashMap<>();

    @Override
    public void preCompute(Map<String, Bars> intradayBarsMap,
                           Map<String, Bars> dailyBarsMap,
                           int orBarCount) {
        rankLookup.clear();

        // Step 1: For each scrip, slice intraday bars into days and compute returns
        // Collect: date -> list of (symbol, return)
        Map<LocalDate, List<SymbolReturn>> dayReturns = new LinkedHashMap<>();

        for (Map.Entry<String, Bars> entry : intradayBarsMap.entrySet()) {
            String symbol = entry.getKey();
            Bars intradayBars = entry.getValue();
            Bars dailyBars = (dailyBarsMap != null) ? dailyBarsMap.get(symbol) : null;

            // Slice intraday bars into days
            List<DaySlice> days = sliceByDay(intradayBars);

            // Build daily date-index map for prior-close lookups
            Map<LocalDate, Integer> dailyDateIndex = null;
            if (dailyBars != null) {
                dailyDateIndex = buildDailyDateIndex(dailyBars);
            }

            for (DaySlice day : days) {
                if (day.barCount < orBarCount) continue;

                double ret = computeReturn(symbol, intradayBars, day.startIdx,
                        day.barCount, orBarCount, dailyBars, dailyDateIndex, day.date);

                if (!Double.isNaN(ret)) {
                    dayReturns.computeIfAbsent(day.date, d -> new ArrayList<>())
                            .add(new SymbolReturn(symbol, ret));
                }
            }
        }

        // Step 2: For each day, rank the returns as percentiles
        for (Map.Entry<LocalDate, List<SymbolReturn>> entry : dayReturns.entrySet()) {
            LocalDate date = entry.getKey();
            List<SymbolReturn> returns = entry.getValue();

            if (returns.size() < 2) continue; // need at least 2 scrips to rank

            // Sort by return ascending
            returns.sort(Comparator.comparingDouble(sr -> sr.ret));

            int total = returns.size();
            for (int i = 0; i < total; i++) {
                // Percentile rank: how many are below / (total - 1) * 100
                double rank = (total > 1) ? (double) i / (total - 1) * 100.0 : 50.0;
                rankLookup.put(key(date, returns.get(i).symbol), rank);
            }
        }
    }

    @Override
    public double compute(TradeContext ctx) {
        LocalDate date = Instant.ofEpochMilli(ctx.trade().date())
                .atZone(IST).toLocalDate();
        Double rank = rankLookup.get(key(date, ctx.trade().symbol()));
        return (rank != null) ? rank : Double.NaN;
    }

    /**
     * Compute the return for a single scrip on a single day.
     *
     * @param symbol          scrip ID
     * @param intradayBars    intraday bars for this scrip
     * @param dayStart        first bar index of the day
     * @param dayCount        number of bars in the day
     * @param orBarCount      number of bars forming the opening range
     * @param dailyBars       daily bars for this scrip (may be null)
     * @param dailyDateIndex  date -> index map for daily bars (may be null)
     * @param date            the trading date
     * @return the return value, or Double.NaN if unavailable
     */
    protected abstract double computeReturn(String symbol, Bars intradayBars,
                                             int dayStart, int dayCount, int orBarCount,
                                             Bars dailyBars, Map<LocalDate, Integer> dailyDateIndex,
                                             LocalDate date);

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String key(LocalDate date, String symbol) {
        return date.toString() + '|' + symbol;
    }

    record DaySlice(LocalDate date, int startIdx, int barCount) {}
    record SymbolReturn(String symbol, double ret) {}

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
                days.add(new DaySlice(currentDate, dayStart, i - dayStart));
                dayStart = i;
                currentDate = barDate;
            }
        }
        days.add(new DaySlice(currentDate, dayStart, bars.size() - dayStart));
        return days;
    }

    static Map<LocalDate, Integer> buildDailyDateIndex(Bars dailyBars) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (int i = 0; i < dailyBars.size(); i++) {
            LocalDate date = Instant.ofEpochMilli(dailyBars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            map.put(date, i);
        }
        return map;
    }
}
