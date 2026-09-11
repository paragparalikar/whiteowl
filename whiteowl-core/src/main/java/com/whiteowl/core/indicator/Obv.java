package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Obv {

    public static float[] compute(float[] close, long[] volume, int size) {
        float[] result = new float[size];
        if (size == 0) return result;
        result[0] = volume[0];
        for (int i = 1; i < size; i++) {
            if (close[i] > close[i - 1]) {
                result[i] = result[i - 1] + volume[i];
            } else if (close[i] < close[i - 1]) {
                result[i] = result[i - 1] - volume[i];
            } else {
                result[i] = result[i - 1];
            }
        }
        return result;
    }

}
