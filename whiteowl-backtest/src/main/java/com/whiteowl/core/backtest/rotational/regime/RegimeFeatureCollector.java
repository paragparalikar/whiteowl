package com.whiteowl.core.backtest.rotational.regime;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;

/**
 * Computes daily regime features from market data using backward-looking windows only
 * (no lookahead bias). All features use data available before the trading day's open.
 *
 * <h3>Features computed:</h3>
 * <ol>
 *   <li><b>continuation_rate_20d</b> — % of prior 20 days where the Nifty 50's
 *       first-30-min direction matched the day's close direction.</li>
 *   <li><b>realized_vol_20d</b> — Annualized std dev of Nifty 50 daily log-returns
 *       over the prior 20 trading days.</li>
 *   <li><b>cross_sectional_vol_20d</b> — Average cross-sectional std dev of universe
 *       stock returns over the prior 20 trading days.</li>
 *   <li><b>gap_fill_rate_20d</b> — % of gaps that were filled (traded back through
 *       prior close) over the prior 20 trading days across the universe.</li>
 *   <li><b>nifty_sma_slope_20d</b> — Annualized slope of Nifty 50's 20-day SMA,
 *       measured as the 10-day rate of change of the SMA.</li>
 * </ol>
 *
 * <p>All features are keyed by {@link LocalDate} and can be looked up per trading day.</p>
 */
@Slf4j
public final class RegimeFeatureCollector {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int LOOKBACK = 20;

    /** Pre-computed regime features per date. */
    private final Map<LocalDate, RegimeFeatures> featuresByDate = new LinkedHashMap<>();

    /**
     * Pre-compute all regime features from available data. Must be called before
     * the backtest runs.
     *
     * @param niftyDailyBars  Nifty 50 daily bars
     * @param niftyIntradayBars  Nifty 50 intraday (5-min) bars (for continuation rate; null to skip)
     * @param universeDailyBars  per-stock daily bars for the universe (for dispersion and gap fill)
     */
    public void preCompute(Bars niftyDailyBars,
                           Bars niftyIntradayBars,
                           Map<String, Bars> universeDailyBars) {

        // Build Nifty daily date→index map
        int niftySize = niftyDailyBars.size();
        BarsArrays niftyArr = niftyDailyBars.arrays();
        Map<LocalDate, Integer> niftyDateIdx = new LinkedHashMap<>();
        LocalDate[] niftyDates = new LocalDate[niftySize];
        for (int i = 0; i < niftySize; i++) {
            LocalDate d = Instant.ofEpochMilli(niftyDailyBars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            niftyDateIdx.put(d, i);
            niftyDates[i] = d;
        }

        // Pre-compute Nifty daily log returns
        double[] niftyLogReturns = new double[niftySize];
        for (int i = 1; i < niftySize; i++) {
            niftyLogReturns[i] = Math.log(niftyArr.close()[i] / niftyArr.close()[i - 1]);
        }

        // Pre-compute Nifty 20-day SMA
        float[] niftySma20 = IndicatorFunctions.sma(niftyArr.close(), niftySize, 20);

        // Pre-compute Nifty intraday continuation data
        // For each Nifty daily date, determine if first-30-min direction matched day close direction
        boolean[] continuationFlags = null;
        if (niftyIntradayBars != null && niftyIntradayBars.size() > 0) {
            continuationFlags = computeContinuationFlags(niftyIntradayBars, niftyDailyBars, niftyDateIdx);
        }

        // Build universe daily return arrays and date index maps
        Map<String, double[]> universeReturns = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Integer>> universeDateIdx = new LinkedHashMap<>();
        for (Map.Entry<String, Bars> entry : universeDailyBars.entrySet()) {
            Bars bars = entry.getValue();
            int size = bars.size();
            if (size < 2) continue;
            BarsArrays arr = bars.arrays();
            double[] returns = new double[size];
            Map<LocalDate, Integer> dateIdx = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                LocalDate d = Instant.ofEpochMilli(bars.getTimestamp(i)).atZone(IST).toLocalDate();
                dateIdx.put(d, i);
                if (i > 0) {
                    returns[i] = (arr.close()[i] - arr.close()[i - 1]) / arr.close()[i - 1];
                }
            }
            universeReturns.put(entry.getKey(), returns);
            universeDateIdx.put(entry.getKey(), dateIdx);
        }

        // Compute features for each Nifty trading date
        for (int dayIdx = LOOKBACK + 10; dayIdx < niftySize; dayIdx++) {
            LocalDate date = niftyDates[dayIdx];

            // 1. Continuation rate (prior 20 days)
            double continuationRate = Double.NaN;
            if (continuationFlags != null) {
                int count = 0, hits = 0;
                for (int k = dayIdx - LOOKBACK; k < dayIdx; k++) {
                    if (k >= 0 && k < continuationFlags.length) {
                        count++;
                        if (continuationFlags[k]) hits++;
                    }
                }
                continuationRate = count > 0 ? (double) hits / count : Double.NaN;
            }

            // 2. Realized volatility (prior 20 days)
            double realizedVol = computeRealizedVol(niftyLogReturns, dayIdx, LOOKBACK);

            // 3. Cross-sectional volatility (prior 20 days)
            double crossSectionalVol = computeCrossSectionalVol(
                    date, dayIdx, niftyDates, universeReturns, universeDateIdx);

            // 4. Gap fill rate (prior 20 days)
            double gapFillRate = computeGapFillRate(
                    date, dayIdx, niftyDates, universeDailyBars, universeDateIdx);

            // 5. Nifty SMA slope (20-day SMA, 10-day rate of change)
            double smaSlope = Double.NaN;
            if (dayIdx >= 10 && niftySma20[dayIdx - 1] > 0 && niftySma20[dayIdx - 11] > 0) {
                // Use prior day's SMA values (available before today's open)
                smaSlope = (niftySma20[dayIdx - 1] / niftySma20[dayIdx - 11] - 1) * 25.2;
            }

            featuresByDate.put(date, new RegimeFeatures(
                    continuationRate, realizedVol, crossSectionalVol, gapFillRate, smaSlope));
        }

        log.info("Regime features computed for {} trading days", featuresByDate.size());
    }

    /**
     * Get regime features for a specific date.
     * @return features or null if not available for that date
     */
    public RegimeFeatures getFeatures(LocalDate date) {
        return featuresByDate.get(date);
    }

    /** All dates with computed features. */
    public Set<LocalDate> getDates() {
        return featuresByDate.keySet();
    }

    /** All features as an ordered map. */
    public Map<LocalDate, RegimeFeatures> getAllFeatures() {
        return Collections.unmodifiableMap(featuresByDate);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * For each Nifty daily bar, check if the first-30-min direction matched the day close.
     * Returns a boolean array aligned with niftyDailyBars indices.
     */
    private boolean[] computeContinuationFlags(Bars intradayBars, Bars dailyBars,
                                                Map<LocalDate, Integer> dailyDateIdx) {
        int dailySize = dailyBars.size();
        boolean[] flags = new boolean[dailySize];

        // Slice intraday bars by day
        Map<LocalDate, List<Integer>> intradayByDate = new LinkedHashMap<>();
        for (int i = 0; i < intradayBars.size(); i++) {
            LocalDate d = Instant.ofEpochMilli(intradayBars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            intradayByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<LocalDate, List<Integer>> entry : intradayByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Integer> barIndices = entry.getValue();
            Integer dIdx = dailyDateIdx.get(date);
            if (dIdx == null || barIndices.size() < 7) continue; // need at least 6 bars for 30 min

            // First-30-min direction: sign of (bar[5].close - bar[0].open) for 5-min bars
            // 6 bars = 30 minutes (9:15-9:45)
            int firstBarIdx = barIndices.get(0);
            int thirtyMinBarIdx = barIndices.get(Math.min(5, barIndices.size() - 1));
            float open = intradayBars.getOpen(firstBarIdx);
            float thirtyMinClose = intradayBars.getClose(thirtyMinBarIdx);
            boolean firstHalfUp = thirtyMinClose > open;

            // Day close direction: sign of (close - open) from daily bar
            float dayOpen = dailyBars.getOpen(dIdx);
            float dayClose = dailyBars.getClose(dIdx);
            boolean dayUp = dayClose > dayOpen;

            flags[dIdx] = (firstHalfUp == dayUp);
        }

        return flags;
    }

    /**
     * Annualized realized volatility from daily log-returns over a lookback window.
     */
    private double computeRealizedVol(double[] logReturns, int endIdx, int lookback) {
        int start = endIdx - lookback;
        if (start < 1) return Double.NaN;

        double sum = 0, sumSq = 0;
        int count = 0;
        for (int i = start; i < endIdx; i++) {
            sum += logReturns[i];
            sumSq += logReturns[i] * logReturns[i];
            count++;
        }
        if (count < 2) return Double.NaN;

        double mean = sum / count;
        double variance = sumSq / count - mean * mean;
        return Math.sqrt(variance * 252); // annualized
    }

    /**
     * Average cross-sectional standard deviation of universe stock daily returns
     * over the prior lookback days.
     */
    private double computeCrossSectionalVol(LocalDate today, int niftyDayIdx,
                                             LocalDate[] niftyDates,
                                             Map<String, double[]> universeReturns,
                                             Map<String, Map<LocalDate, Integer>> universeDateIdx) {
        double totalCsVol = 0;
        int validDays = 0;

        for (int k = niftyDayIdx - LOOKBACK; k < niftyDayIdx; k++) {
            if (k < 0 || k >= niftyDates.length) continue;
            LocalDate d = niftyDates[k];

            // Collect returns for all stocks on date d
            List<Double> returns = new ArrayList<>();
            for (Map.Entry<String, double[]> entry : universeReturns.entrySet()) {
                Map<LocalDate, Integer> dateIdx = universeDateIdx.get(entry.getKey());
                Integer idx = dateIdx.get(d);
                if (idx != null && idx > 0) {
                    double r = entry.getValue()[idx];
                    if (!Double.isNaN(r) && Math.abs(r) < 0.50) { // filter extreme outliers
                        returns.add(r);
                    }
                }
            }

            if (returns.size() >= 10) {
                double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double variance = returns.stream()
                        .mapToDouble(r -> (r - mean) * (r - mean))
                        .average().orElse(0);
                totalCsVol += Math.sqrt(variance);
                validDays++;
            }
        }

        return validDays > 0 ? totalCsVol / validDays : Double.NaN;
    }

    /**
     * Gap fill rate: fraction of gaps that were filled (price traded back through
     * prior close) over the prior lookback days across the universe.
     */
    private double computeGapFillRate(LocalDate today, int niftyDayIdx,
                                       LocalDate[] niftyDates,
                                       Map<String, Bars> universeDailyBars,
                                       Map<String, Map<LocalDate, Integer>> universeDateIdx) {
        int totalGaps = 0;
        int filledGaps = 0;

        for (int k = niftyDayIdx - LOOKBACK; k < niftyDayIdx; k++) {
            if (k < 0 || k >= niftyDates.length) continue;
            LocalDate d = niftyDates[k];

            for (Map.Entry<String, Bars> entry : universeDailyBars.entrySet()) {
                Map<LocalDate, Integer> dateIdx = universeDateIdx.get(entry.getKey());
                if (dateIdx == null) continue;
                Integer idx = dateIdx.get(d);
                if (idx == null || idx < 1) continue;

                Bars bars = entry.getValue();
                float priorClose = bars.getClose(idx - 1);
                float todayOpen = bars.getOpen(idx);
                float todayHigh = bars.getHigh(idx);
                float todayLow = bars.getLow(idx);

                if (priorClose <= 0 || todayOpen <= 0) continue;

                float gapPct = (todayOpen - priorClose) / priorClose;
                if (Math.abs(gapPct) < 0.005) continue; // ignore tiny gaps < 0.5%

                totalGaps++;

                // Gap-up filled if low traded below prior close
                // Gap-down filled if high traded above prior close
                if (gapPct > 0 && todayLow <= priorClose) {
                    filledGaps++;
                } else if (gapPct < 0 && todayHigh >= priorClose) {
                    filledGaps++;
                }
            }
        }

        return totalGaps > 0 ? (double) filledGaps / totalGaps : Double.NaN;
    }
}
