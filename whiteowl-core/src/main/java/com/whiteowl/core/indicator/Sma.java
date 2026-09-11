package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sma {

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
        result[period - 1] = sum / period;
        for (int i = period; i < size; i++) {
            sum += source[i] - source[i - period];
            result[i] = sum / period;
        }
        return result;
    }

}
