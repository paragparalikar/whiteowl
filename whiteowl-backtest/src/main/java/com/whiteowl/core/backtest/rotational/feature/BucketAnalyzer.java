package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.backtest.rotational.RotationalTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Reusable feature analyzer that performs:
 * <ol>
 *   <li>Distribution stats (min, P10, median, mean, P90, max)</li>
 *   <li>Equal-width bucket analysis (trades, wins, P&L, win% per bucket)</li>
 *   <li>Winner vs Loser comparison</li>
 *   <li>Min-threshold sweep (filter trades below threshold)</li>
 *   <li>Max-threshold sweep (filter trades above threshold)</li>
 *   <li>Best min+max range combination search</li>
 * </ol>
 *
 * <p>Works with any feature — just specify the feature name and bucket width.</p>
 */
public final class BucketAnalyzer implements FeatureAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(BucketAnalyzer.class);

    private final String featureName;
    private final double bucketWidth;
    private final double minSweepStart;
    private final double minSweepEnd;
    private final double minSweepStep;
    private final double maxSweepStart;
    private final double maxSweepEnd;
    private final double maxSweepStep;
    private final int minTradesForSignificance;

    /**
     * Create a bucket analyzer with default sweep ranges.
     *
     * @param featureName the feature name (must match a key in the feature map)
     * @param bucketWidth width of each bucket for the bucket analysis
     */
    public BucketAnalyzer(String featureName, double bucketWidth) {
        this(featureName, bucketWidth,
                0.0, 3.0, 0.1,   // min sweep
                0.5, 5.0, 0.1,   // max sweep
                20);
    }

    /**
     * Create a bucket analyzer with custom sweep ranges.
     */
    public BucketAnalyzer(String featureName, double bucketWidth,
                          double minSweepStart, double minSweepEnd, double minSweepStep,
                          double maxSweepStart, double maxSweepEnd, double maxSweepStep,
                          int minTradesForSignificance) {
        this.featureName = featureName;
        this.bucketWidth = bucketWidth;
        this.minSweepStart = minSweepStart;
        this.minSweepEnd = minSweepEnd;
        this.minSweepStep = minSweepStep;
        this.maxSweepStart = maxSweepStart;
        this.maxSweepEnd = maxSweepEnd;
        this.maxSweepStep = maxSweepStep;
        this.minTradesForSignificance = minTradesForSignificance;
    }

    @Override
    public void analyze(List<RotationalTrade> trades, List<Map<String, Double>> featureValues) {
        // Collect valid (featureValue, netPnl) pairs
        List<double[]> data = new ArrayList<>();
        for (int i = 0; i < trades.size(); i++) {
            Double val = featureValues.get(i).get(featureName);
            if (val == null || Double.isNaN(val)) continue;
            data.add(new double[]{val, trades.get(i).netPnl()});
        }

        if (data.isEmpty()) {
            log.warn("No valid {} values to analyze.", featureName);
            return;
        }

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  {} Analysis", featureName);
        log.info("══════════════════════════════════════════════════════════════");
        log.info("");
        log.info("Valid trades: {} / {}", data.size(), trades.size());
        log.info("");

        // Distribution
        double[] values = data.stream().mapToDouble(d -> d[0]).toArray();
        printDistribution(values);

        // Bucket analysis
        printBucketAnalysis(data, values);

        // Winner vs Loser
        printWinnerVsLoser(data);

        // Min sweep
        printMinSweep(data);

        // Max sweep
        printMaxSweep(data);

        // Best combination
        printBestCombination(data);
    }

    private void printDistribution(double[] values) {
        log.info("── Distribution ────────────────────────────────────────────");
        log.info("Min:    {}", fmt(Arrays.stream(values).min().orElse(0)));
        log.info("P10:    {}", fmt(percentile(values, 10)));
        log.info("Median: {}", fmt(percentile(values, 50)));
        log.info("Mean:   {}", fmt(Arrays.stream(values).average().orElse(0)));
        log.info("P90:    {}", fmt(percentile(values, 90)));
        log.info("Max:    {}", fmt(Arrays.stream(values).max().orElse(0)));
        log.info("");
    }

    private void printBucketAnalysis(List<double[]> data, double[] values) {
        double min = Arrays.stream(values).min().orElse(0);
        double max = Arrays.stream(values).max().orElse(0);
        double bStart = Math.floor(min / bucketWidth) * bucketWidth;
        double bEnd = Math.ceil(max / bucketWidth) * bucketWidth;

        log.info("── Bucket Analysis ({} width) ────────────────────────────", fmt(bucketWidth));
        log.info("{}", String.format("%-18s %8s %8s %12s %12s %10s",
                featureName, "Trades", "Wins", "Total PnL", "Avg PnL", "Win%"));

        for (double lo = bStart; lo < bEnd; lo += bucketWidth) {
            double hi = lo + bucketWidth;
            final double flo = lo, fhi = hi;
            double[] bucket = data.stream()
                    .filter(d -> d[0] >= flo && d[0] < fhi)
                    .mapToDouble(d -> d[1]).toArray();
            if (bucket.length == 0) continue;

            BucketStats s = computeStats(bucket);
            log.info("{}", String.format("[%6.2f, %6.2f) %8d %8d %12.0f %12.0f %9.1f%%",
                    lo, hi, bucket.length, s.wins, s.totalPnl, s.avgPnl, s.winRate));
        }
        log.info("");
    }

    private void printWinnerVsLoser(List<double[]> data) {
        double[] winnerVals = data.stream().filter(d -> d[1] > 0).mapToDouble(d -> d[0]).toArray();
        double[] loserVals = data.stream().filter(d -> d[1] <= 0).mapToDouble(d -> d[0]).toArray();

        log.info("── Winner vs Loser {} ──────────────────────────────", featureName);
        log.info("{}", String.format("%-12s %8s %10s %10s %10s %10s %10s",
                "", "Count", "Mean", "Median", "P10", "P90", "StdDev"));
        if (winnerVals.length > 0) {
            log.info("{}", String.format("%-12s %8d %10s %10s %10s %10s %10s",
                    "Winners", winnerVals.length,
                    fmt(mean(winnerVals)), fmt(percentile(winnerVals, 50)),
                    fmt(percentile(winnerVals, 10)), fmt(percentile(winnerVals, 90)),
                    fmt(stddev(winnerVals))));
        }
        if (loserVals.length > 0) {
            log.info("{}", String.format("%-12s %8d %10s %10s %10s %10s %10s",
                    "Losers", loserVals.length,
                    fmt(mean(loserVals)), fmt(percentile(loserVals, 50)),
                    fmt(percentile(loserVals, 10)), fmt(percentile(loserVals, 90)),
                    fmt(stddev(loserVals))));
        }
        log.info("");
    }

    private void printMinSweep(List<double[]> data) {
        log.info("── Min {} Sweep (filter below threshold) ──────────", featureName);
        log.info("{}", String.format("%-18s %8s %8s %12s %10s",
                "Min " + featureName, "Trades", "Wins", "Total PnL", "Win%"));
        for (double t = minSweepStart; t <= minSweepEnd; t += minSweepStep) {
            final double threshold = t;
            double[] filtered = data.stream().filter(d -> d[0] >= threshold)
                    .mapToDouble(d -> d[1]).toArray();
            if (filtered.length == 0) continue;
            BucketStats s = computeStats(filtered);
            log.info("{}", String.format(">= %-14s %8d %8d %12.0f %9.1f%%",
                    fmt(t), filtered.length, s.wins, s.totalPnl, s.winRate));
        }
        log.info("");
    }

    private void printMaxSweep(List<double[]> data) {
        log.info("── Max {} Sweep (filter above threshold) ──────────", featureName);
        log.info("{}", String.format("%-18s %8s %8s %12s %10s",
                "Max " + featureName, "Trades", "Wins", "Total PnL", "Win%"));
        for (double t = maxSweepStart; t <= maxSweepEnd; t += maxSweepStep) {
            final double threshold = t;
            double[] filtered = data.stream().filter(d -> d[0] <= threshold)
                    .mapToDouble(d -> d[1]).toArray();
            if (filtered.length == 0) continue;
            BucketStats s = computeStats(filtered);
            log.info("{}", String.format("<= %-14s %8d %8d %12.0f %9.1f%%",
                    fmt(t), filtered.length, s.wins, s.totalPnl, s.winRate));
        }
        log.info("");
    }

    private void printBestCombination(List<double[]> data) {
        log.info("── Best Min+Max {} Combination ─────────────────", featureName);
        double bestAvgPnl = Double.NEGATIVE_INFINITY;
        double bestMin = 0, bestMax = 10;
        int bestCount = 0;
        double bestWinRate = 0;

        for (double mn = minSweepStart; mn <= minSweepEnd; mn += minSweepStep) {
            for (double mx = mn + 2 * minSweepStep; mx <= maxSweepEnd; mx += maxSweepStep) {
                final double fmn = mn, fmx = mx;
                double[] filtered = data.stream()
                        .filter(d -> d[0] >= fmn && d[0] <= fmx)
                        .mapToDouble(d -> d[1]).toArray();
                if (filtered.length < minTradesForSignificance) continue;

                BucketStats s = computeStats(filtered);
                if (s.avgPnl > bestAvgPnl) {
                    bestAvgPnl = s.avgPnl;
                    bestMin = mn;
                    bestMax = mx;
                    bestCount = filtered.length;
                    bestWinRate = s.winRate;
                }
            }
        }

        log.info("Best range: [{}, {}]", fmt(bestMin), fmt(bestMax));
        log.info("  Trades:   {}", bestCount);
        log.info("  Avg PnL:  {}", fmt(bestAvgPnl));
        log.info("  Win rate: {}%", fmt(bestWinRate));
        log.info("");
    }

    // ── Stats helpers ────────────────────────────────────────────────────

    private record BucketStats(int wins, double totalPnl, double avgPnl, double winRate) {}

    private static BucketStats computeStats(double[] pnlValues) {
        int wins = 0;
        double totalPnl = 0;
        for (double v : pnlValues) {
            totalPnl += v;
            if (v > 0) wins++;
        }
        double avgPnl = pnlValues.length > 0 ? totalPnl / pnlValues.length : 0;
        double winRate = pnlValues.length > 0 ? (double) wins / pnlValues.length * 100 : 0;
        return new BucketStats(wins, totalPnl, avgPnl, winRate);
    }

    static double percentile(double[] values, double p) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        double idx = (p / 100.0) * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted[lo];
        return sorted[lo] + (idx - lo) * (sorted[hi] - sorted[lo]);
    }

    static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    static double stddev(double[] values) {
        double m = mean(values);
        double sumSq = 0;
        for (double v : values) sumSq += (v - m) * (v - m);
        return Math.sqrt(sumSq / values.length);
    }

    static String fmt(double value) {
        return String.format("%.2f", value);
    }
}
