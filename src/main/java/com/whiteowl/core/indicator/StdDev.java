package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StdDev {

    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        if (period > size) return result;
        float sum = 0f;
        for (int i = 0; i < period; i++) {
            sum += source[i];
        }
        for (int i = period - 1; i < size; i++) {
            if (i > period - 1) {
                sum += source[i] - source[i - period];
            }
            float mean = sum / period;
            float sqSum = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float diff = source[j] - mean;
                sqSum += diff * diff;
            }
            result[i] = (float) Math.sqrt(sqSum / period);
        }
        return result;
    }

}
