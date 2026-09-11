package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StochRsi {

    public static float[][] compute(float[] close, int size, int rsiPeriod, int stochPeriod,
                                    int kSmooth, int dSmooth) {
        float[] rsi = Rsi.compute(close, size, rsiPeriod);
        float[] stochK = new float[size];
        int warmup = rsiPeriod + stochPeriod - 1;
        for (int i = 0; i < Math.min(warmup, size); i++) {
            stochK[i] = Float.NaN;
        }
        for (int i = warmup; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - stochPeriod + 1; j <= i; j++) {
                if (Float.isNaN(rsi[j])) continue;
                if (rsi[j] > highest) highest = rsi[j];
                if (rsi[j] < lowest) lowest = rsi[j];
            }
            float range = highest - lowest;
            stochK[i] = range > 0 ? ((rsi[i] - lowest) / range) * 100f : 50f;
        }
        float[] k = Sma.compute(stochK, size, kSmooth);
        float[] d = Sma.compute(k, size, dSmooth);
        return new float[][]{k, d};
    }

}
