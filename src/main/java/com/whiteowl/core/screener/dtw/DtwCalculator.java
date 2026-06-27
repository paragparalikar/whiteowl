package com.whiteowl.core.screener.dtw;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DtwCalculator {

    public static double subsequenceDtw(float[] template, float[] candidate, int bandWidthPct) {
        int m = template.length;
        int n = candidate.length;
        int band = Math.max(1, m * bandWidthPct / 100);
        double[] prev = new double[m + 1];
        double[] curr = new double[m + 1];
        for (int i = 0; i <= m; i++) {
            prev[i] = Double.MAX_VALUE;
        }
        prev[0] = 0.0;
        double globalMin = Double.MAX_VALUE;
        for (int j = 1; j <= n; j++) {
            curr[0] = 0.0;
            for (int i = 1; i <= m; i++) {
                if (Math.abs(i - (int) ((long) j * m / n)) > band) {
                    curr[i] = Double.MAX_VALUE;
                    continue;
                }
                double cost = squaredDiff(template[i - 1], candidate[j - 1]);
                double insertion = prev[i];
                double deletion = curr[i - 1];
                double match = prev[i - 1];
                curr[i] = cost + min3(insertion, deletion, match);
            }
            globalMin = Math.min(globalMin, curr[m]);
            double[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return Math.sqrt(globalMin / m);
    }

    public static float[] normalize(float[] series, int from, int length) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        int to = from + length;
        for (int i = from; i < to; i++) {
            min = Math.min(min, series[i]);
            max = Math.max(max, series[i]);
        }
        float range = max - min;
        float[] result = new float[length];
        if (range == 0f) {
            return result;
        }
        for (int i = 0; i < length; i++) {
            result[i] = (series[from + i] - min) / range;
        }
        return result;
    }

    private static double squaredDiff(float a, float b) {
        double d = a - b;
        return d * d;
    }

    private static double min3(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }

}
