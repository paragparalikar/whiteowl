package com.whiteowl.scripting.ranker.patternmatch;

import java.util.Arrays;

final class Dtw {

    private Dtw() {
    }

    static double compute(float[] a, float[] b) {
        int m = a.length;
        int n = b.length;
        if (m == 0 || n == 0) return Double.MAX_VALUE;
        double[] prev = new double[n + 1];
        double[] curr = new double[n + 1];
        Arrays.fill(prev, Double.MAX_VALUE);
        prev[0] = 0;
        for (int i = 1; i <= m; i++) {
            curr[0] = Double.MAX_VALUE;
            for (int j = 1; j <= n; j++) {
                double cost = Math.abs(a[i - 1] - b[j - 1]);
                curr[j] = cost + Math.min(Math.min(prev[j], curr[j - 1]), prev[j - 1]);
            }
            double[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[n];
    }

}
