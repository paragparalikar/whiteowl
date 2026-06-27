package com.whiteowl.core.backtest.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Ema {

    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < period - 1 && i < size; i++) {
            result[i] = Float.NaN;
        }
        if (period > size) return result;
        float sum = 0f;
        for (int i = 0; i < period; i++) {
            sum += source[i];
        }
        float multiplier = 2.0f / (period + 1);
        result[period - 1] = sum / period;
        for (int i = period; i < size; i++) {
            result[i] = (source[i] - result[i - 1]) * multiplier + result[i - 1];
        }
        return result;
    }

}
