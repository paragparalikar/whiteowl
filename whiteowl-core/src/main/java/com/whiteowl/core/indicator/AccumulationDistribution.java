package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccumulationDistribution {

    public static float[] compute(float[] high, float[] low, float[] close,
                                  long[] volume, int size) {
        float[] result = new float[size];
        if (size == 0) return result;
        float ad = 0f;
        for (int i = 0; i < size; i++) {
            float hl = high[i] - low[i];
            float mfm = hl > 0 ? ((close[i] - low[i]) - (high[i] - close[i])) / hl : 0f;
            ad += mfm * volume[i];
            result[i] = ad;
        }
        return result;
    }

}
