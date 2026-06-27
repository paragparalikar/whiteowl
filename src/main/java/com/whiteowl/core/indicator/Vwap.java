package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Vwap {

    public static float[] compute(float[] high, float[] low, float[] close,
                                  long[] volume, int size) {
        float[] result = new float[size];
        if (size == 0) return result;
        double cumTpVol = 0;
        double cumVol = 0;
        for (int i = 0; i < size; i++) {
            double tp = (high[i] + low[i] + close[i]) / 3.0;
            cumTpVol += tp * volume[i];
            cumVol += volume[i];
            result[i] = cumVol > 0 ? (float) (cumTpVol / cumVol) : (float) tp;
        }
        return result;
    }

}
