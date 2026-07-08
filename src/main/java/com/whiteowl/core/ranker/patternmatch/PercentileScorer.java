package com.whiteowl.core.ranker.patternmatch;

import java.util.Arrays;

final class PercentileScorer {

    private static final int DEFAULT_SAMPLE_COUNT = 100;

    private double[] baselineDistances;

    PercentileScorer() {
    }

    void buildBaseline(float[][] normalizedPatterns, DistanceMetric metric) {
        int patternCount = normalizedPatterns.length;
        if (patternCount < 2) {
            baselineDistances = null;
            return;
        }
        int pairCount = patternCount * (patternCount - 1) / 2;
        int sampleSize = Math.min(pairCount, DEFAULT_SAMPLE_COUNT);
        baselineDistances = new double[sampleSize];
        int idx = 0;
        for (int i = 0; i < patternCount && idx < sampleSize; i++) {
            for (int j = i + 1; j < patternCount && idx < sampleSize; j++) {
                baselineDistances[idx++] = computeDistance(normalizedPatterns[i], normalizedPatterns[j], metric);
            }
        }
        if (idx < sampleSize) {
            baselineDistances = Arrays.copyOf(baselineDistances, idx);
        }
        Arrays.sort(baselineDistances);
    }

    double toPercentile(double rawDistance) {
        if (baselineDistances == null || baselineDistances.length == 0) {
            return rawDistance;
        }
        int pos = Arrays.binarySearch(baselineDistances, rawDistance);
        if (pos < 0) pos = -(pos + 1);
        return (double) pos / baselineDistances.length * 100.0;
    }

    private static double computeDistance(float[] a, float[] b, DistanceMetric metric) {
        return switch (metric) {
            case SUBSEQUENCE_DTW -> SubsequenceDtw.compute(a, b);
            case SBD -> ShapeBasedDistance.compute(a, b);
        };
    }

}
