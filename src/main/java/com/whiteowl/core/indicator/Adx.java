package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Adx {

    private static final float HUNDRED = 100f;

    public static float[][] compute(float[] high, float[] low, float[] close, int size, int period) {
        float[] adx = new float[size];
        float[] plusDi = new float[size];
        float[] minusDi = new float[size];
        int required = period * 2 + 1;
        if (size < required) {
            initNaN(adx, size);
            initNaN(plusDi, size);
            initNaN(minusDi, size);
            return new float[][]{adx, plusDi, minusDi};
        }
        initNaN(adx, period);
        initNaN(plusDi, period);
        initNaN(minusDi, period);
        float smoothedPlusDm = 0f;
        float smoothedMinusDm = 0f;
        float smoothedTr = 0f;
        for (int i = 1; i <= period; i++) {
            smoothedTr += trueRange(high, low, close, i);
            smoothedPlusDm += plusDm(high, i);
            smoothedMinusDm += minusDm(high, low, i);
        }
        plusDi[period] = smoothedTr == 0 ? 0 : (smoothedPlusDm / smoothedTr) * HUNDRED;
        minusDi[period] = smoothedTr == 0 ? 0 : (smoothedMinusDm / smoothedTr) * HUNDRED;
        adx[period] = Float.NaN;
        float adxSum = 0f;
        for (int i = period + 1; i < size; i++) {
            smoothedTr = smoothedTr - (smoothedTr / period) + trueRange(high, low, close, i);
            smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / period) + plusDm(high, i);
            smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / period) + minusDm(high, low, i);
            float pdi = smoothedTr == 0 ? 0 : (smoothedPlusDm / smoothedTr) * HUNDRED;
            float mdi = smoothedTr == 0 ? 0 : (smoothedMinusDm / smoothedTr) * HUNDRED;
            plusDi[i] = pdi;
            minusDi[i] = mdi;
            float diSum = pdi + mdi;
            float dx = diSum == 0 ? 0 : (Math.abs(pdi - mdi) / diSum) * HUNDRED;
            if (i < period * 2) {
                adxSum += dx;
                adx[i] = Float.NaN;
            } else if (i == period * 2) {
                adxSum += dx;
                adx[i] = adxSum / period;
            } else {
                adx[i] = (adx[i - 1] * (period - 1) + dx) / period;
            }
        }
        return new float[][]{adx, plusDi, minusDi};
    }

    private static float trueRange(float[] high, float[] low, float[] close, int i) {
        float hl = high[i] - low[i];
        float hc = Math.abs(high[i] - close[i - 1]);
        float lc = Math.abs(low[i] - close[i - 1]);
        return Math.max(hl, Math.max(hc, lc));
    }

    private static float plusDm(float[] high, int i) {
        float upMove = high[i] - high[i - 1];
        float downMove = 0;
        return (upMove > downMove && upMove > 0) ? upMove : 0;
    }

    private static float minusDm(float[] high, float[] low, int i) {
        float upMove = high[i] - high[i - 1];
        float downMove = low[i - 1] - low[i];
        return (downMove > upMove && downMove > 0) ? downMove : 0;
    }

    private static void initNaN(float[] arr, int count) {
        for (int i = 0; i < count; i++) {
            arr[i] = Float.NaN;
        }
    }

}
