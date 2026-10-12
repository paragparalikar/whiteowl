package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sma {

    /**
     * Simple moving average. NaN-safe: a window that contains any NaN
     * produces NaN, but the running sum recovers once the window is fully
     * populated again (a plain running sum would stay NaN forever).
     */
    public static float[] compute(float[] source, int size, int period) {
        float[] result = new float[size];
        if (period > size) {
            for (int i = 0; i < size; i++) result[i] = Float.NaN;
            return result;
        }
        float sum = 0f;
        int valid = 0;
        for (int i = 0; i < size; i++) {
            float v = source[i];
            if (!Float.isNaN(v)) {
                sum += v;
                valid++;
            }
            if (i >= period) {
                float out = source[i - period];
                if (!Float.isNaN(out)) {
                    sum -= out;
                    valid--;
                }
            }
            result[i] = (i >= period - 1 && valid == period)
                    ? sum / period : Float.NaN;
        }
        return result;
    }

}
