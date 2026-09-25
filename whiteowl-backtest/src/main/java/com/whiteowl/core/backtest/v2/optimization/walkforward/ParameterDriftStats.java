package com.whiteowl.core.backtest.v2.optimization.walkforward;

import java.util.List;

/**
 * Distribution of one parameter's selected value across walk-forward windows.
 * A high coefficient of variation means the optimum wanders — the strategy is
 * unstable, e.g. selections 9,9,12,9,12 (CV≈0.16, stable) vs 3,21,6,18,12
 * (CV≈0.60, unstable).
 */
public record ParameterDriftStats(
        String parameter,
        int samples,
        double mean,
        double median,
        double stdDev,
        double min,
        double max,
        double coefficientOfVariation,
        boolean stable) {

    public static ParameterDriftStats of(String parameter, List<Double> values,
                                          double cvThreshold) {
        if (values.isEmpty()) {
            return new ParameterDriftStats(parameter, 0,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, false);
        }
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double sum = 0, min = sorted[0], max = sorted[sorted.length - 1];
        for (double v : sorted) sum += v;
        double mean = sum / sorted.length;
        double sumSq = 0;
        for (double v : sorted) {
            double d = v - mean;
            sumSq += d * d;
        }
        double std = sorted.length > 1 ? Math.sqrt(sumSq / (sorted.length - 1)) : 0;
        double median = sorted.length % 2 == 1
                ? sorted[sorted.length / 2]
                : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2.0;
        double cv = Math.abs(mean) > 1e-9 ? std / Math.abs(mean) : (std > 0 ? Double.MAX_VALUE : 0);
        return new ParameterDriftStats(parameter, sorted.length, mean, median,
                std, min, max, cv, cv <= cvThreshold);
    }

}
