package com.whiteowl.core.backtest.v2.engine;

import com.whiteowl.core.bar.model.BarsArrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Wilder ATR computed over raw bar arrays. Element {@code i} uses only bars
 * {@code <= i} — no look-ahead. Entries before the warmup period are NaN.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AtrSeries {

    public static float[] compute(BarsArrays a, int period) {
        int n = a.size();
        float[] atr = new float[n];
        if (n == 0 || period <= 0) {
            return atr;
        }
        float[] tr = new float[n];
        tr[0] = a.high()[0] - a.low()[0];
        for (int i = 1; i < n; i++) {
            float hl = a.high()[i] - a.low()[i];
            float hc = Math.abs(a.high()[i] - a.close()[i - 1]);
            float lc = Math.abs(a.low()[i] - a.close()[i - 1]);
            tr[i] = Math.max(hl, Math.max(hc, lc));
        }
        for (int i = 0; i < n; i++) {
            if (i < period - 1) {
                atr[i] = Float.NaN;
            } else if (i == period - 1) {
                float sum = 0f;
                for (int j = 0; j <= i; j++) sum += tr[j];
                atr[i] = sum / period;
            } else {
                atr[i] = (atr[i - 1] * (period - 1) + tr[i]) / period;
            }
        }
        return atr;
    }

}
