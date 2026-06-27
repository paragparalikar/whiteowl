package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChaikinMoneyFlow {

    public static float[] compute(float[] high, float[] low, float[] close,
                                  long[] volume, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float mfvSum = 0f;
            float volSum = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float hl = high[j] - low[j];
                float mfm = hl > 0 ? ((close[j] - low[j]) - (high[j] - close[j])) / hl : 0f;
                mfvSum += mfm * volume[j];
                volSum += volume[j];
            }
            result[i] = volSum > 0 ? mfvSum / volSum : 0f;
        }
        return result;
    }

}
