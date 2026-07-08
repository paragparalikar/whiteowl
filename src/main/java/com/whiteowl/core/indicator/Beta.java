package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Beta {

    private static final float HUNDRED = 100f;

    public static float[] compute(float[] source, float[] benchmark, int size, int period) {
        float[] result = new float[size];
        int required = period + 1;
        for (int i = 0; i < Math.min(required - 1, size); i++) {
            result[i] = Float.NaN;
        }
        if (size < required) return result;
        for (int i = required - 1; i < size; i++) {
            float sumSrcRet = 0f;
            float sumBmkRet = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float srcRet = returnOf(source, j);
                float bmkRet = returnOf(benchmark, j);
                sumSrcRet += srcRet;
                sumBmkRet += bmkRet;
            }
            float meanSrcRet = sumSrcRet / period;
            float meanBmkRet = sumBmkRet / period;
            float covariance = 0f;
            float variance = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float srcRet = returnOf(source, j);
                float bmkRet = returnOf(benchmark, j);
                float bmkDiff = bmkRet - meanBmkRet;
                covariance += (srcRet - meanSrcRet) * bmkDiff;
                variance += bmkDiff * bmkDiff;
            }
            result[i] = variance == 0f ? 0f : covariance / variance;
        }
        return result;
    }

    private static float returnOf(float[] prices, int i) {
        float prev = prices[i - 1];
        return prev != 0f ? ((prices[i] - prev) / prev) * HUNDRED : 0f;
    }

}
