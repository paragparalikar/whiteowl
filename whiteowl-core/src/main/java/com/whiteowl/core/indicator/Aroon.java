package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Aroon {

    private static final float HUNDRED = 100f;

    public static float[][] compute(float[] high, float[] low, int size, int period) {
        float[] aroonUp = new float[size];
        float[] aroonDown = new float[size];
        for (int i = 0; i < Math.min(period, size); i++) {
            aroonUp[i] = Float.NaN;
            aroonDown[i] = Float.NaN;
        }
        for (int i = period; i < size; i++) {
            int highIdx = 0;
            int lowIdx = 0;
            float highestVal = Float.MIN_VALUE;
            float lowestVal = Float.MAX_VALUE;
            for (int j = 0; j <= period; j++) {
                int idx = i - period + j;
                if (high[idx] >= highestVal) {
                    highestVal = high[idx];
                    highIdx = j;
                }
                if (low[idx] <= lowestVal) {
                    lowestVal = low[idx];
                    lowIdx = j;
                }
            }
            aroonUp[i] = (HUNDRED * highIdx) / period;
            aroonDown[i] = (HUNDRED * lowIdx) / period;
        }
        return new float[][]{aroonUp, aroonDown};
    }

}
