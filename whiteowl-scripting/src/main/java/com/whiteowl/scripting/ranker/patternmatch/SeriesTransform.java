package com.whiteowl.scripting.ranker.patternmatch;

import com.whiteowl.core.bar.model.Bars;

final class SeriesTransform {

    private SeriesTransform() {
    }

    static float[] extractClose(Bars bars, int count) {
        float[] close = new float[count];
        for (int i = 0; i < count; i++) {
            close[i] = bars.getClose(i);
        }
        return close;
    }

    static float[] extractTail(float[] source, int count) {
        float[] tail = new float[count];
        System.arraycopy(source, source.length - count, tail, 0, count);
        return tail;
    }

    static float[] toLogReturns(float[] prices) {
        if (prices.length < 2) return new float[0];
        float[] returns = new float[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = (float) Math.log(prices[i] / prices[i - 1]);
        }
        return returns;
    }

    static float[] zScoreNormalize(float[] values) {
        if (values.length == 0) return values;
        float sum = 0;
        for (float v : values) {
            sum += v;
        }
        float mean = sum / values.length;
        float varianceSum = 0;
        for (float v : values) {
            float diff = v - mean;
            varianceSum += diff * diff;
        }
        float stddev = (float) Math.sqrt(varianceSum / values.length);
        float[] normalized = new float[values.length];
        if (stddev == 0) {
            return normalized;
        }
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (values[i] - mean) / stddev;
        }
        return normalized;
    }

}
