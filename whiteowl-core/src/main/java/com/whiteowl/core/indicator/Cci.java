package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Cci {

    private static final float CONSTANT = 0.015f;

    public static float[] compute(float[] high, float[] low, float[] close, int size, int period) {
        float[] tp = new float[size];
        for (int i = 0; i < size; i++) {
            tp[i] = (high[i] + low[i] + close[i]) / 3f;
        }
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float sum = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                sum += tp[j];
            }
            float mean = sum / period;
            float meanDev = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                meanDev += Math.abs(tp[j] - mean);
            }
            meanDev /= period;
            result[i] = meanDev != 0 ? (tp[i] - mean) / (CONSTANT * meanDev) : 0f;
        }
        return result;
    }

}
