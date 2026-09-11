package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;

/**
 * Enriched ranker that scores breakout candidates using three factors:
 *
 * <ul>
 *   <li><b>|Gap ATR|</b> — absolute gap (today open - prior close) as a multiple of ATR(14).
 *       Higher magnitude = more conviction, regardless of direction.</li>
 *   <li><b>OR RVOL</b> — relative volume during the opening range vs the 20-day average.
 *       Higher = more institutional participation.</li>
 *   <li><b>RS Rank</b> — cross-sectional percentile rank (0-100) of each scrip's return
 *       from prior close to OR-end, among all scrips that day.</li>
 * </ul>
 *
 * <h3>Scoring</h3>
 * <pre>
 *   Long score  = |gapAtr| * orRvol * rsRank
 *   Short score = |gapAtr| * orRvol / rsRank
 * </pre>
 *
 * <p>Long candidates are picked from upward breakouts, sorted by long score descending.
 * Short candidates are picked from downward breakouts, sorted by short score descending.
 * This ensures that both longs and shorts are the most "explosive" setups of the day,
 * with RS rank acting as the directional tiebreaker.</p>
 */
public final class GapRvolRsRanker implements RotationalBacktestEngine.EnrichedRanker {

    private static final Logger log = LoggerFactory.getLogger(GapRvolRsRanker.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int RVOL_LOOKBACK = 20;
    private static final double RS_RANK_FLOOR = 1.0; // clamp to avoid divide-by-zero

    // Pre-computed: "date|symbol" -> RS rank percentile (0-100)
    private final Map<String, Double> rsRankLookup = new HashMap<>();

    @Override
    public void init(Map<String, Bars> intradayBars,
                     Map<String, Bars> dailyBars,
                     int orBarCount) {
        this.storedOrBarCount = orBarCount;
        rsRankLookup.clear();
        preComputeRsRank(intradayBars, dailyBars, orBarCount);
        log.info("GapRvolRsRanker initialized — RS rank lookup: {} entries", rsRankLookup.size());
    }

    @Override
    public List<RotationalBacktestEngine.RankedPick> rank(
            LocalDate date,
            Map<String, OrbSimulator.OrbResult> orbResults,
            Map<String, RotationalBacktestEngine.DaySlice> daySlices,
            Map<String, Bars> intradayBars,
            Map<String, Bars> dailyBars,
            Map<String, float[]> dailyAtrMap,
            Map<String, Map<LocalDate, Integer>> dailyDateIdx,
            int picks) {

        List<ScoredCandidate> candidates = new ArrayList<>();

        for (Map.Entry<String, OrbSimulator.OrbResult> entry : orbResults.entrySet()) {
            String symbol = entry.getKey();
            OrbSimulator.OrbResult orb = entry.getValue();

            // 1. Compute |gapAtr|
            double gapAtr = computeGapAtr(symbol, date, daySlices, intradayBars,
                    dailyAtrMap, dailyDateIdx, dailyBars);
            if (Double.isNaN(gapAtr)) continue;
            double absGapAtr = Math.abs(gapAtr);
            if (absGapAtr <= 0) continue;

            // 2. Compute OR RVOL
            double orRvol = computeOrRvol(symbol, daySlices, intradayBars,
                    storedOrBarCount);
            if (Double.isNaN(orRvol) || orRvol <= 0) continue;

            // 3. Look up RS rank
            double rsRank = rsRankLookup.getOrDefault(key(date, symbol), Double.NaN);
            if (Double.isNaN(rsRank)) continue;
            rsRank = Math.max(rsRank, RS_RANK_FLOOR);

            // 4. Score — use rsRank for longs, 1/rsRank for shorts
            double score = (orb.side() == RotationalTrade.Side.LONG)
                    ? absGapAtr * orRvol * rsRank
                    : absGapAtr * orRvol / rsRank;

            candidates.add(new ScoredCandidate(symbol, score));
        }

        // Sort by score descending and pick top N
        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        List<RotationalBacktestEngine.RankedPick> result = new ArrayList<>();
        for (int i = 0; i < Math.min(picks, candidates.size()); i++) {
            ScoredCandidate c = candidates.get(i);
            OrbSimulator.OrbResult orb = orbResults.get(c.symbol);
            result.add(new RotationalBacktestEngine.RankedPick(
                    c.symbol, orb.side(), c.score));
        }

        return result;
    }

    // ── RS Rank pre-computation ──────────────────────────────────────────

    /**
     * Pre-compute RS rank: for each trading day, compute the return from
     * prior close to OR-end for every scrip, then rank as percentile (0-100).
     */
    private void preComputeRsRank(Map<String, Bars> intradayBarsMap,
                                   Map<String, Bars> dailyBarsMap,
                                   int orBarCount) {
        // date -> list of (symbol, return)
        Map<LocalDate, List<SymbolReturn>> dayReturns = new LinkedHashMap<>();

        for (Map.Entry<String, Bars> entry : intradayBarsMap.entrySet()) {
            String symbol = entry.getKey();
            Bars intradayBars = entry.getValue();
            Bars dailyBars = (dailyBarsMap != null) ? dailyBarsMap.get(symbol) : null;
            if (dailyBars == null) continue;

            Map<LocalDate, Integer> dailyDateIndex = buildDailyDateIndex(dailyBars);
            List<DaySlice> days = sliceByDay(intradayBars);

            for (DaySlice day : days) {
                if (day.barCount < orBarCount) continue;

                Integer dayIdx = dailyDateIndex.get(day.date);
                if (dayIdx == null || dayIdx < 1) continue;

                float priorClose = dailyBars.getClose(dayIdx - 1);
                if (priorClose <= 0 || Float.isNaN(priorClose)) continue;

                int orEndIdx = day.startIdx + orBarCount - 1;
                float orEndClose = intradayBars.getClose(orEndIdx);
                if (Float.isNaN(orEndClose) || orEndClose <= 0) continue;

                double ret = (orEndClose - priorClose) / priorClose;
                dayReturns.computeIfAbsent(day.date, d -> new ArrayList<>())
                        .add(new SymbolReturn(symbol, ret));
            }
        }

        // Rank returns within each day
        for (Map.Entry<LocalDate, List<SymbolReturn>> entry : dayReturns.entrySet()) {
            LocalDate date = entry.getKey();
            List<SymbolReturn> returns = entry.getValue();
            if (returns.size() < 2) continue;

            returns.sort(Comparator.comparingDouble(sr -> sr.ret));
            int total = returns.size();
            for (int i = 0; i < total; i++) {
                double rank = (total > 1) ? (double) i / (total - 1) * 100.0 : 50.0;
                rsRankLookup.put(key(date, returns.get(i).symbol), rank);
            }
        }
    }

    // ── Per-trade feature computation ────────────────────────────────────

    private double computeGapAtr(String symbol, LocalDate date,
                                  Map<String, RotationalBacktestEngine.DaySlice> daySlices,
                                  Map<String, Bars> intradayBars,
                                  Map<String, float[]> dailyAtrMap,
                                  Map<String, Map<LocalDate, Integer>> dailyDateIdx,
                                  Map<String, Bars> dailyBars) {
        float[] atrArr = dailyAtrMap.get(symbol);
        Map<LocalDate, Integer> dateIdx = dailyDateIdx.get(symbol);
        if (atrArr == null || dateIdx == null) return Double.NaN;

        Integer dayIdx = dateIdx.get(date);
        if (dayIdx == null || dayIdx < 1) return Double.NaN;

        float priorDayAtr = atrArr[dayIdx - 1];
        if (Float.isNaN(priorDayAtr) || priorDayAtr <= 0) return Double.NaN;

        Bars db = dailyBars != null ? dailyBars.get(symbol) : null;
        if (db == null) return Double.NaN;
        float priorClose = db.getClose(dayIdx - 1);
        if (Float.isNaN(priorClose) || priorClose <= 0) return Double.NaN;

        RotationalBacktestEngine.DaySlice ds = daySlices.get(symbol);
        if (ds == null) return Double.NaN;
        Bars bars = intradayBars.get(symbol);
        if (bars == null) return Double.NaN;
        float todayOpen = bars.getOpen(ds.startIdx());

        return (todayOpen - priorClose) / priorDayAtr;
    }

    private int storedOrBarCount;

    private double computeOrRvol(String symbol,
                                  Map<String, RotationalBacktestEngine.DaySlice> daySlices,
                                  Map<String, Bars> intradayBars,
                                  int orBarCount) {
        RotationalBacktestEngine.DaySlice ds = daySlices.get(symbol);
        if (ds == null || ds.barCount() < orBarCount) return Double.NaN;

        Bars bars = intradayBars.get(symbol);
        if (bars == null) return Double.NaN;
        int dayStart = ds.startIdx();

        // Today's OR volume
        long todayOrVol = 0;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            todayOrVol += bars.getVolume(i);
        }
        if (todayOrVol <= 0) return Double.NaN;

        // Walk backward to find prior days' OR volumes
        long totalPriorVol = 0;
        int priorDaysFound = 0;
        int scanIdx = dayStart - 1;

        while (scanIdx >= 0 && priorDaysFound < RVOL_LOOKBACK) {
            LocalDate barDate = Instant.ofEpochMilli(bars.getTimestamp(scanIdx))
                    .atZone(IST).toLocalDate();

            int priorDayStart = scanIdx;
            while (priorDayStart > 0) {
                LocalDate prevDate = Instant.ofEpochMilli(bars.getTimestamp(priorDayStart - 1))
                        .atZone(IST).toLocalDate();
                if (!prevDate.equals(barDate)) break;
                priorDayStart--;
            }

            int priorDayCount = scanIdx + 1 - priorDayStart;
            if (priorDayCount >= orBarCount) {
                long priorOrVol = 0;
                for (int i = priorDayStart; i < priorDayStart + orBarCount; i++) {
                    priorOrVol += bars.getVolume(i);
                }
                if (priorOrVol > 0) {
                    totalPriorVol += priorOrVol;
                    priorDaysFound++;
                }
            }
            scanIdx = priorDayStart - 1;
        }

        if (priorDaysFound == 0) return Double.NaN;
        double avgPriorOrVol = (double) totalPriorVol / priorDaysFound;
        return todayOrVol / avgPriorOrVol;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private record ScoredCandidate(String symbol, double score) {}
    private record SymbolReturn(String symbol, double ret) {}
    private record DaySlice(LocalDate date, int startIdx, int barCount) {}

    private static String key(LocalDate date, String symbol) {
        return date.toString() + '|' + symbol;
    }

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
