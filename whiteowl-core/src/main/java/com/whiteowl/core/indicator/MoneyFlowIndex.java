package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoneyFlowIndex {

    private static final float HUNDRED = 100f;

    public static float[] compute(float[] high, float[] low, float[] close,
                                  long[] volume, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period, size); i++) {
            result[i] = Float.NaN;
        }
        if (size < 2) return result;
        float[] tp = new float[size];
        for (int i = 0; i < size; i++) {
            tp[i] = (high[i] + low[i] + close[i]) / 3f;
        }
        float[] rawMf = new float[size];
        for (int i = 0; i < size; i++) {
            rawMf[i] = tp[i] * volume[i];
        }
        for (int i = period; i < size; i++) {
            float posMf = 0f;
            float negMf = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                if (tp[j] > tp[j - 1]) {
                    posMf += rawMf[j];
                } else if (tp[j] < tp[j - 1]) {
                    negMf += rawMf[j];
                }
            }
            if (negMf == 0) {
                result[i] = HUNDRED;
            } else {
                float mfr = posMf / negMf;
                result[i] = HUNDRED - HUNDRED / (1f + mfr);
            }
        }
        return result;
    }

}
