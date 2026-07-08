package com.whiteowl.core.ranker.patternmatch;

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

    static float[] toDerivative(float[] series) {
        if (series.length < 2) return new float[0];
        float[] derivative = new float[series.length - 1];
        for (int i = 1; i < series.length; i++) {
            derivative[i - 1] = series[i] - series[i - 1];
        }
        return derivative;
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

    static float[][] extractMultiChannel(Bars bars, int startIdx, int length) {
        float[] close = new float[length];
        float[] volume = new float[length];
        float[] range = new float[length];
        for (int i = 0; i < length; i++) {
            int idx = startIdx + i;
            close[i] = bars.getClose(idx);
            volume[i] = bars.getVolume(idx);
            range[i] = bars.getHigh(idx) - bars.getLow(idx);
        }
        return new float[][]{close, volume, range};
    }

}
