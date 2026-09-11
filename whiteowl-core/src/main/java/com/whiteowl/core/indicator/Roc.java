package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Roc {

    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period, size); i++) {
            result[i] = Float.NaN;
        }
        for (int i = period; i < size; i++) {
            float prev = source[i - period];
            result[i] = prev != 0 ? ((source[i] - prev) / prev) * 100f : 0f;
        }
        return result;
    }

}
