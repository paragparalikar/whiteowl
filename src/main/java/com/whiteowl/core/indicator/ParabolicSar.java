package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParabolicSar {

    public static float[][] compute(float[] high, float[] low, float[] close, int size,
                                    float afStart, float afStep, float afMax) {
        float[] sar = new float[size];
        boolean[] bullish = new boolean[size];
        if (size < 2) {
            for (int i = 0; i < size; i++) sar[i] = Float.NaN;
            return new float[][]{sar, toFloat(bullish, size)};
        }
        boolean isBullish = close[1] >= close[0];
        float af = afStart;
        float ep;
        if (isBullish) {
            sar[0] = low[0];
            ep = high[0];
        } else {
            sar[0] = high[0];
            ep = low[0];
        }
        bullish[0] = isBullish;
        for (int i = 1; i < size; i++) {
            float prevSar = sar[i - 1];
            float newSar = prevSar + af * (ep - prevSar);
            if (isBullish) {
                newSar = Math.min(newSar, Math.min(low[i - 1], i >= 2 ? low[i - 2] : low[i - 1]));
                if (low[i] < newSar) {
                    isBullish = false;
                    newSar = ep;
                    ep = low[i];
                    af = afStart;
                } else {
                    if (high[i] > ep) {
                        ep = high[i];
                        af = Math.min(af + afStep, afMax);
                    }
                }
            } else {
                newSar = Math.max(newSar, Math.max(high[i - 1], i >= 2 ? high[i - 2] : high[i - 1]));
                if (high[i] > newSar) {
                    isBullish = true;
                    newSar = ep;
                    ep = high[i];
                    af = afStart;
                } else {
                    if (low[i] < ep) {
                        ep = low[i];
                        af = Math.min(af + afStep, afMax);
                    }
                }
            }
            sar[i] = newSar;
            bullish[i] = isBullish;
        }
        return new float[][]{sar, toFloat(bullish, size)};
    }

    private static float[] toFloat(boolean[] arr, int size) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = arr[i] ? 1f : 0f;
        }
        return result;
    }

}
