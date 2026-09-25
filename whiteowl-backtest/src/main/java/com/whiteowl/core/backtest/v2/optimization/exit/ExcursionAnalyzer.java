package com.whiteowl.core.backtest.v2.optimization.exit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Statistical analysis of an {@link Excursion} population: percentile
 * distributions used to propose candidate stops, targets, and time stops.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExcursionAnalyzer {

    public record Percentiles(double p5, double p10, double p25, double p50,
                              double p75, double p90, double p95) {
    }

    public static Percentiles percentiles(List<Excursion> trades,
                                           ToDoubleFunction<Excursion> field) {
        double[] v = trades.stream().mapToDouble(field).filter(d -> !Double.isNaN(d))
                .sorted().toArray();
        if (v.length == 0) {
            return new Percentiles(Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN);
        }
        return new Percentiles(q(v, 0.05), q(v, 0.10), q(v, 0.25), q(v, 0.50),
                q(v, 0.75), q(v, 0.90), q(v, 0.95));
    }

    /**
     * Candidate initial stops (ATR multiples) from the MAE distribution:
     * quartile-ish ladder over the observed adverse-excursion range, clipped to
     * [{@code min}, {@code max}] at {@code step} resolution.
     */
    public static double[] candidateStops(List<Excursion> trades,
                                           double min, double max, double step) {
        Percentiles p = percentiles(trades, Excursion::maeAtr);
        double lo = Double.isNaN(p.p25) ? min : Math.max(min, Math.floor(p.p25 / step) * step);
        double hi = Double.isNaN(p.p95) ? max : Math.min(max, Math.ceil(p.p95 / step) * step);
        return ladder(lo, hi, step);
    }

    /** Candidate targets (ATR multiples) from the MFE distribution. */
    public static double[] candidateTargets(List<Excursion> trades,
                                             double min, double max, double step) {
        Percentiles p = percentiles(trades, Excursion::mfeAtr);
        double lo = Double.isNaN(p.p25) ? min : Math.max(min, Math.floor(p.p25 / step) * step);
        double hi = Double.isNaN(p.p95) ? max : Math.min(max, Math.ceil(p.p95 / step) * step);
        return ladder(lo, hi, step);
    }

    /** Candidate time stops (bars) from the time-to-MFE distribution. */
    public static int[] candidateTimeStops(List<Excursion> trades,
                                            int min, int max, int step) {
        Percentiles p = percentiles(trades, Excursion::mfeBar);
        int lo = Double.isNaN(p.p10) ? min : Math.max(min, (int) Math.floor(p.p10));
        int hi = Double.isNaN(p.p75) ? max : Math.min(max, (int) Math.ceil(p.p75));
        int count = Math.max(1, (hi - lo) / step + 1);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = lo + i * step;
        return out;
    }

    private static double[] ladder(double min, double max, double step) {
        if (step <= 0 || max < min) {
            return new double[]{min};
        }
        int count = (int) Math.floor((max - min) / step + 1e-9) + 1;
        double[] out = new double[Math.max(1, count)];
        for (int i = 0; i < out.length; i++) {
            out[i] = min + i * step;
        }
        return out;
    }

    private static double q(double[] sorted, double p) {
        if (sorted.length == 0) return Double.NaN;
        double idx = p * (sorted.length - 1);
        int lo = (int) idx;
        int hi = Math.min(lo + 1, sorted.length - 1);
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo);
    }

    /** Debug helper: percentile summary of an excursion population. */
    public static String summarize(List<Excursion> trades) {
        Percentiles mae = percentiles(trades, Excursion::maeAtr);
        Percentiles mfe = percentiles(trades, Excursion::mfeAtr);
        return String.format(
                "trades=%d | MAE_ATR p50=%.2f p75=%.2f p90=%.2f | MFE_ATR p50=%.2f p75=%.2f p90=%.2f",
                trades.size(), mae.p50(), mae.p75(), mae.p90(),
                mfe.p50(), mfe.p75(), mfe.p90());
    }

}
