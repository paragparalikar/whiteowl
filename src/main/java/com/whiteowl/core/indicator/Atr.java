package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Atr {

    public static float[] compute(float[] high, float[] low, float[] close, int size, int period) {
        float[] result = new float[size];
        if (size < 2) return result;
        result[0] = Float.NaN;
        for (int i = 1; i < Math.min(period, size); i++) {
            result[i] = Float.NaN;
        }
        if (period >= size) return result;
        float sum = high[0] - low[0];
        for (int i = 1; i < period; i++) {
            sum += trueRange(high[i], low[i], close[i - 1]);
        }
        result[period] = sum / period;
        for (int i = period + 1; i < size; i++) {
            float tr = trueRange(high[i], low[i], close[i - 1]);
            result[i] = (result[i - 1] * (period - 1) + tr) / period;
        }
        return result;
    }

    private static float trueRange(float high, float low, float prevClose) {
        float hl = high - low;
        float hc = Math.abs(high - prevClose);
        float lc = Math.abs(low - prevClose);
        return Math.max(hl, Math.max(hc, lc));
    }

}
