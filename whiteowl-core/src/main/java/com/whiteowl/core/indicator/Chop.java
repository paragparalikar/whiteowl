package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Chop {

    private static final float HUNDRED = 100f;

    /**
     * Choppiness Index: {@code 100 * log10(sum(TR, n) / (highestHigh(n) - lowestLow(n))) / log10(n)}.
     * Values near 100 indicate a choppy/ranging market, near 0 a trending one.
     */
    public static float[] compute(float[] high, float[] low, float[] close, int size, int period) {
        float[] result = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            result[i] = Float.NaN;
        }
        if (size == 0 || period > size) {
            return result;
        }
        float[] tr = new float[size];
        tr[0] = high[0] - low[0];
        for (int i = 1; i < size; i++) {
            float hl = high[i] - low[i];
            float hc = Math.abs(high[i] - close[i - 1]);
            float lc = Math.abs(low[i] - close[i - 1]);
            tr[i] = Math.max(hl, Math.max(hc, lc));
        }
        double logPeriod = Math.log10(period);
        for (int i = period - 1; i < size; i++) {
            float trSum = 0f;
            float hh = Float.MIN_VALUE;
            float ll = Float.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                trSum += tr[j];
                hh = Math.max(hh, high[j]);
                ll = Math.min(ll, low[j]);
            }
            float range = hh - ll;
            result[i] = range > 0f
                    ? (float) (HUNDRED * Math.log10(trSum / range) / logPeriod)
                    : Float.NaN;
        }
        return result;
    }

}
