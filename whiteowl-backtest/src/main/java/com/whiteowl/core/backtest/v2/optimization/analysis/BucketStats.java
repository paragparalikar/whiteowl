package com.whiteowl.core.backtest.v2.optimization.analysis;

import java.util.List;

/**
 * Trade statistics for one bucket of a feature (a bin of feature values or a
 * calendar category). Trade-level "sortino" here is
 * {@code mean(pnl%) / downsideStdDev(pnl%)} — a per-trade ratio, not the
 * annualized curve metric.
 */
public record BucketStats(
        double lowerBound,
        double upperBound,
        String label,
        int tradeCount,
        double winRate,
        double avgPnlPercent,
        double medianPnlPercent,
        double avgR,
        double sortino,
        double profitFactor) {

    public static BucketStats of(double lo, double hi, String label,
                                  List<TradeObservation> trades) {
        if (trades.isEmpty()) {
            return new BucketStats(lo, hi, label, 0, 0, 0, 0, 0, 0, 0);
        }
        int wins = 0;
        double sum = 0, winSum = 0, lossSum = 0, rSum = 0;
        int rCount = 0;
        double[] pcts = new double[trades.size()];
        for (int i = 0; i < trades.size(); i++) {
            TradeObservation t = trades.get(i);
            pcts[i] = t.netPnlPercent();
            sum += pcts[i];
            if (pcts[i] > 0) {
                wins++;
                winSum += pcts[i];
            } else {
                lossSum += pcts[i];
            }
            if (!Float.isNaN(t.rMultiple())) {
                rSum += t.rMultiple();
                rCount++;
            }
        }
        double mean = sum / trades.size();
        double downSq = 0;
        int downN = 0;
        for (double p : pcts) {
            if (p < 0) {
                downSq += p * p;
                downN++;
            }
        }
        double downsideStd = downN > 1 ? Math.sqrt(downSq / (downN - 1)) : 0;
        double sortino = downsideStd > 0 ? mean / downsideStd : 0;
        double pf = lossSum != 0 ? winSum / Math.abs(lossSum)
                : (winSum > 0 ? Double.POSITIVE_INFINITY : 0);
        java.util.Arrays.sort(pcts);
        double median = pcts.length % 2 == 1 ? pcts[pcts.length / 2]
                : (pcts[pcts.length / 2 - 1] + pcts[pcts.length / 2]) / 2;
        return new BucketStats(lo, hi, label, trades.size(),
                (double) wins / trades.size() * 100, mean, median,
                rCount > 0 ? rSum / rCount : Double.NaN, sortino, pf);
    }

}
