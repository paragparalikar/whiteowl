package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Ichimoku {

    public static float[][] compute(float[] high, float[] low, float[] close, int size,
                                    int tenkanPeriod, int kijunPeriod, int senkouBPeriod, int displacement) {
        float[] tenkan = midLine(high, low, size, tenkanPeriod);
        float[] kijun = midLine(high, low, size, kijunPeriod);
        float[] senkouA = new float[size + displacement];
        float[] senkouB = new float[size + displacement];
        float[] chikou = new float[size];
        initNaN(senkouA, 0, senkouA.length);
        initNaN(senkouB, 0, senkouB.length);
        initNaN(chikou, 0, size);
        for (int i = 0; i < size; i++) {
            if (!Float.isNaN(tenkan[i]) && !Float.isNaN(kijun[i])) {
                int shifted = i + displacement;
                if (shifted < senkouA.length) {
                    senkouA[shifted] = (tenkan[i] + kijun[i]) / 2f;
                }
            }
        }
        float[] midLong = midLine(high, low, size, senkouBPeriod);
        for (int i = 0; i < size; i++) {
            if (!Float.isNaN(midLong[i])) {
                int shifted = i + displacement;
                if (shifted < senkouB.length) {
                    senkouB[shifted] = midLong[i];
                }
            }
        }
        for (int i = displacement; i < size; i++) {
            chikou[i - displacement] = close[i];
        }
        float[] senkouATrimmed = new float[size];
        float[] senkouBTrimmed = new float[size];
        System.arraycopy(senkouA, 0, senkouATrimmed, 0, size);
        System.arraycopy(senkouB, 0, senkouBTrimmed, 0, size);
        return new float[][]{tenkan, kijun, senkouATrimmed, senkouBTrimmed, chikou};
    }

    private static float[] midLine(float[] high, float[] low, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                if (high[j] > highest) highest = high[j];
                if (low[j] < lowest) lowest = low[j];
            }
            result[i] = (highest + lowest) / 2f;
        }
        return result;
    }

    private static void initNaN(float[] arr, int from, int to) {
        for (int i = from; i < to; i++) {
            arr[i] = Float.NaN;
        }
    }

}
