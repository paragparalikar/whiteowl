package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;

/**
 * A configurable ranker that scores breakout candidates using a selectable
 * subset of the three factors: |Gap ATR|, OR RVOL, and RS Rank.
 *
 * <p>This extends the logic of {@link GapRvolRsRanker} but allows any
 * combination of factors to be enabled or disabled for comparison studies.</p>
 *
 * <h3>Scoring</h3>
 * <pre>
 *   base = (useGap ? |gapAtr| : 1) * (useRvol ? orRvol : 1)
 *   Long score  = base * (useRs ? rsRank : 1)
 *   Short score = base * (useRs ? 1/rsRank : 1)
 * </pre>
 *
 * <p>When all three factors are enabled, this produces identical results
 * to {@link GapRvolRsRanker}.</p>
 */
public final class ConfigurableRanker implements RotationalBacktestEngine.EnrichedRanker {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableRanker.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int RVOL_LOOKBACK = 20;
    private static final double RS_RANK_FLOOR = 1.0;

    private final boolean useGap;
    private final boolean useRvol;
    private final boolean useRs;
    private final String label;

    private int storedOrBarCount;
    private final Map<String, Double> rsRankLookup = new HashMap<>();

    public ConfigurableRanker(boolean useGap, boolean useRvol, boolean useRs) {
        this.useGap = useGap;
        this.useRvol = useRvol;
        this.useRs = useRs;

        // Build a human-readable label
        List<String> parts = new ArrayList<>();
        if (useGap) parts.add("Gap");
        if (useRvol) parts.add("RVOL");
        if (useRs) parts.add("RS");
        this.label = parts.isEmpty() ? "None" : String.join("+", parts);
    }

    public String getLabel() { return label; }

    public int getStoredOrBarCount() { return storedOrBarCount; }

    @Override
    public void init(Map<String, Bars> intradayBars,
                     Map<String, Bars> dailyBars,
                     int orBarCount) {
        this.storedOrBarCount = orBarCount;
        rsRankLookup.clear();
        if (useRs) {
            preComputeRsRank(intradayBars, dailyBars, orBarCount);
        }
        log.info("ConfigurableRanker [{}] initialized", label);
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

            // Compute factors (only those enabled)
            double gapFactor = 1.0;
            if (useGap) {
                double gapAtr = computeGapAtr(symbol, date, daySlices, intradayBars,
                        dailyAtrMap, dailyDateIdx, dailyBars);
                if (Double.isNaN(gapAtr)) continue;
                gapFactor = Math.abs(gapAtr);
                if (gapFactor <= 0) continue;
            }

            double rvolFactor = 1.0;
            if (useRvol) {
                double orRvol = computeOrRvol(symbol, daySlices, intradayBars,
                        storedOrBarCount);
                if (Double.isNaN(orRvol) || orRvol <= 0) continue;
                rvolFactor = orRvol;
            }

            double rsFactor = 1.0;
            if (useRs) {
                double rsRank = rsRankLookup.getOrDefault(key(date, symbol), Double.NaN);
                if (Double.isNaN(rsRank)) continue;
                rsRank = Math.max(rsRank, RS_RANK_FLOOR);
                rsFactor = (orb.side() == RotationalTrade.Side.LONG) ? rsRank : 1.0 / rsRank;
            }

            double score = gapFactor * rvolFactor * rsFactor;
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

    // ── RS Rank pre-computation (reused from GapRvolRsRanker) ────────────

    private void preComputeRsRank(Map<String, Bars> intradayBarsMap,
                                   Map<String, Bars> dailyBarsMap,
                                   int orBarCount) {
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

    // ── Per-trade feature computation (reused from GapRvolRsRanker) ──────

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

    private double computeOrRvol(String symbol,
                                  Map<String, RotationalBacktestEngine.DaySlice> daySlices,
                                  Map<String, Bars> intradayBars,
                                  int orBarCount) {
        RotationalBacktestEngine.DaySlice ds = daySlices.get(symbol);
        if (ds == null || ds.barCount() < orBarCount) return Double.NaN;

        Bars bars = intradayBars.get(symbol);
        if (bars == null) return Double.NaN;
        int dayStart = ds.startIdx();

        long todayOrVol = 0;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            todayOrVol += bars.getVolume(i);
        }
        if (todayOrVol <= 0) return Double.NaN;

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

    private static List<DaySlice> sliceByDay(Bars bars) {
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

    private static Map<LocalDate, Integer> buildDailyDateIndex(Bars dailyBars) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (int i = 0; i < dailyBars.size(); i++) {
            LocalDate date = Instant.ofEpochMilli(dailyBars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            map.put(date, i);
        }
        return map;
    }
}
