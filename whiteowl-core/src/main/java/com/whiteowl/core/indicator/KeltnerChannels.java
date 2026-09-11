package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeltnerChannels {

    public static float[][] compute(float[] high, float[] low, float[] close, int size,
                                    int emaPeriod, int atrPeriod, float multiplier) {
        float[] middle = Ema.compute(close, size, emaPeriod);
        float[] atr = Atr.compute(high, low, close, size, atrPeriod);
        float[] upper = new float[size];
        float[] lower = new float[size];
        for (int i = 0; i < size; i++) {
            if (Float.isNaN(middle[i]) || Float.isNaN(atr[i])) {
                upper[i] = Float.NaN;
                lower[i] = Float.NaN;
            } else {
                upper[i] = middle[i] + multiplier * atr[i];
                lower[i] = middle[i] - multiplier * atr[i];
            }
        }
        return new float[][]{middle, upper, lower};
    }

}
