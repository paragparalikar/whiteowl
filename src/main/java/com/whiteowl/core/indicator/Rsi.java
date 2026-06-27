package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Rsi {

    private static final float HUNDRED = 100f;

    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i <= period && i < size; i++) {
            result[i] = Float.NaN;
        }
        if (period + 1 > size) return result;
        float avgGain = 0f;
        float avgLoss = 0f;
        for (int i = 1; i <= period; i++) {
            float change = source[i] - source[i - 1];
            avgGain += Math.max(change, 0f);
            avgLoss += Math.max(-change, 0f);
        }
        avgGain /= period;
        avgLoss /= period;
        result[period] = avgLoss == 0f ? HUNDRED : HUNDRED - HUNDRED / (1f + avgGain / avgLoss);
        for (int i = period + 1; i < size; i++) {
            float change = source[i] - source[i - 1];
            avgGain = (avgGain * (period - 1) + Math.max(change, 0f)) / period;
            avgLoss = (avgLoss * (period - 1) + Math.max(-change, 0f)) / period;
            result[i] = avgLoss == 0f ? HUNDRED : HUNDRED - HUNDRED / (1f + avgGain / avgLoss);
        }
        return result;
    }

}
