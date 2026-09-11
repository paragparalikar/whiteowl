package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Highest {

    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < period - 1 && i < size; i++) {
            result[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float max = source[i];
            for (int j = 1; j < period; j++) {
                max = Math.max(max, source[i - j]);
            }
            result[i] = max;
        }
        return result;
    }

}
