package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Stochastics {

    public static float[][] compute(float[] high, float[] low, float[] close,
                                    int size, int kPeriod, int dPeriod) {
        float[] k = new float[size];
        float[] d = new float[size];
        for (int i = 0; i < Math.min(kPeriod - 1, size); i++) {
            k[i] = Float.NaN;
        }
        for (int i = kPeriod - 1; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - kPeriod + 1; j <= i; j++) {
                if (high[j] > highest) highest = high[j];
                if (low[j] < lowest) lowest = low[j];
            }
            float range = highest - lowest;
            k[i] = range > 0 ? ((close[i] - lowest) / range) * 100f : 50f;
        }
        float[] smaK = Sma.compute(k, size, dPeriod);
        System.arraycopy(smaK, 0, d, 0, size);
        return new float[][]{k, d};
    }

}
