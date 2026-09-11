package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UltimateOscillator {

    private static final float HUNDRED = 100f;
    private static final float WEIGHT_SHORT = 4f;
    private static final float WEIGHT_MID = 2f;
    private static final float WEIGHT_LONG = 1f;
    private static final float WEIGHT_SUM = WEIGHT_SHORT + WEIGHT_MID + WEIGHT_LONG;

    public static float[] compute(float[] high, float[] low, float[] close, int size,
                                  int period1, int period2, int period3) {
        float[] result = new float[size];
        int maxPeriod = Math.max(period1, Math.max(period2, period3));
        for (int i = 0; i < Math.min(maxPeriod, size); i++) {
            result[i] = Float.NaN;
        }
        if (size < 2) return result;
        float[] bp = new float[size];
        float[] tr = new float[size];
        bp[0] = 0f;
        tr[0] = high[0] - low[0];
        for (int i = 1; i < size; i++) {
            float prevClose = close[i - 1];
            float trueHigh = Math.max(high[i], prevClose);
            float trueLow = Math.min(low[i], prevClose);
            bp[i] = close[i] - trueLow;
            tr[i] = trueHigh - trueLow;
        }
        for (int i = maxPeriod; i < size; i++) {
            float avg1 = periodAverage(bp, tr, i, period1);
            float avg2 = periodAverage(bp, tr, i, period2);
            float avg3 = periodAverage(bp, tr, i, period3);
            result[i] = HUNDRED * (WEIGHT_SHORT * avg1 + WEIGHT_MID * avg2 + WEIGHT_LONG * avg3) / WEIGHT_SUM;
        }
        return result;
    }

    private static float periodAverage(float[] bp, float[] tr, int end, int period) {
        float bpSum = 0f;
        float trSum = 0f;
        for (int j = end - period + 1; j <= end; j++) {
            bpSum += bp[j];
            trSum += tr[j];
        }
        return trSum != 0 ? bpSum / trSum : 0f;
    }

}
