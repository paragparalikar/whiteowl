package com.whiteowl.scripting.ranker.patternmatch;

final class SubsequenceDtw {

    private static final double DEFAULT_BAND_RATIO = 0.1;

    private SubsequenceDtw() {
    }

    static double compute(float[] query, float[] candidate) {
        return compute(query, candidate, DEFAULT_BAND_RATIO);
    }

    static double compute(float[] query, float[] candidate, double bandRatio) {
        int m = query.length;
        int n = candidate.length;
        if (m == 0 || n == 0) return Double.MAX_VALUE;
        int window = Math.max(1, (int) (m * bandRatio));
        double[] prev = new double[n + 1];
        double[] curr = new double[n + 1];
        for (int j = 0; j <= n; j++) {
            prev[j] = 0;
        }
        for (int i = 1; i <= m; i++) {
            curr[0] = Double.MAX_VALUE;
            for (int j = 1; j <= n; j++) {
                if (Math.abs(i - j) > window && j <= m) {
                    curr[j] = Double.MAX_VALUE;
                    continue;
                }
                double cost = Math.abs(query[i - 1] - candidate[j - 1]);
                curr[j] = cost + Math.min(Math.min(prev[j], curr[j - 1]), prev[j - 1]);
            }
            double[] temp = prev;
            prev = curr;
            curr = temp;
        }
        double best = Double.MAX_VALUE;
        for (int j = 1; j <= n; j++) {
            best = Math.min(best, prev[j]);
        }
        return best;
    }

    static double computeMultiChannel(float[][] query, float[][] candidate, double bandRatio) {
        int channels = query.length;
        double totalDistance = 0;
        for (int c = 0; c < channels; c++) {
            totalDistance += compute(query[c], candidate[c], bandRatio);
        }
        return totalDistance / channels;
    }

    static double computeDerivative(float[] series1, float[] series2, double bandRatio) {
        float[] deriv1 = SeriesTransform.toDerivative(series1);
        float[] deriv2 = SeriesTransform.toDerivative(series2);
        float[] norm1 = SeriesTransform.zScoreNormalize(deriv1);
        float[] norm2 = SeriesTransform.zScoreNormalize(deriv2);
        return compute(norm1, norm2, bandRatio);
    }

}
