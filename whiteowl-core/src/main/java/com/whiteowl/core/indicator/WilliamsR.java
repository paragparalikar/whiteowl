package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WilliamsR {

    public static float[] compute(float[] high, float[] low, float[] close, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                if (high[j] > highest) highest = high[j];
                if (low[j] < lowest) lowest = low[j];
            }
            float range = highest - lowest;
            result[i] = range > 0 ? ((highest - close[i]) / range) * -100f : -50f;
        }
        return result;
    }

}
