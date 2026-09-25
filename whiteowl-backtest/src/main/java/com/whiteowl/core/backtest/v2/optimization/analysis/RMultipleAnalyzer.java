package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7 — R-multiple distribution (R = net P&L / initial risk) statistics.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RMultipleAnalyzer {

    public record Result(double mean, double median, double stdDev,
                         double skewness, double kurtosis,
                         double p5, double p25, double p50, double p75, double p95,
                         List<BucketStats> histogramBins, int samples) {
    }

    public static Result analyze(List<TradeObservation> trades, int binCount) {
        double[] r = trades.stream()
                .mapToDouble(TradeObservation::rMultiple)
                .filter(v -> !Double.isNaN(v)).sorted().toArray();
        if (r.length < 2) {
            return new Result(Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, List.of(), r.length);
        }
        double mean = 0;
        for (double v : r) mean += v;
        mean /= r.length;
        double m2 = 0, m3 = 0, m4 = 0;
        for (double v : r) {
            double d = v - mean;
            m2 += d * d;
            m3 += d * d * d;
            m4 += d * d * d * d;
        }
        double sd = Math.sqrt(m2 / r.length);
        double skew = sd > 0 ? (m3 / r.length) / Math.pow(sd, 3) : 0;
        double kurt = sd > 0 ? (m4 / r.length) / (sd * sd * sd * sd) - 3 : 0;

        double lo = r[0], hi = r[r.length - 1];
        double width = (hi - lo) / binCount;
        List<BucketStats> bins = new ArrayList<>();
        if (width > 0) {
            for (int i = 0; i < binCount; i++) {
                double bLo = lo + i * width;
                double bHi = i == binCount - 1 ? hi + 1e-9 : lo + (i + 1) * width;
                final double flo = bLo, fhi = bHi;
                List<TradeObservation> inBin = trades.stream()
                        .filter(t -> !Float.isNaN(t.rMultiple())
                                && t.rMultiple() >= flo && t.rMultiple() < fhi)
                        .toList();
                bins.add(BucketStats.of(bLo, bHi, null, inBin));
            }
        }
        return new Result(mean, q(r, 0.5), sd, skew, kurt,
                q(r, 0.05), q(r, 0.25), q(r, 0.50), q(r, 0.75), q(r, 0.95),
                bins, r.length);
    }

    /** Descriptive skew classification — not a quality verdict. */
    public static String classifySkew(double skewness) {
        if (Double.isNaN(skewness)) return "INSUFFICIENT DATA";
        if (skewness > 0.5) return "Right-skewed";
        if (skewness < -0.5) return "Left-skewed";
        return "Approximately symmetric";
    }

    private static double q(double[] s, double p) {
        double idx = p * (s.length - 1);
        int lo = (int) idx;
        int hi = Math.min(lo + 1, s.length - 1);
        return s[lo] + (s[hi] - s[lo]) * (idx - lo);
    }

}
