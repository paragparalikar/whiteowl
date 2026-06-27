package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Supertrend {

    public static float[][] compute(float[] high, float[] low, float[] close, int size,
                                    int period, float multiplier) {
        float[] values = new float[size];
        float[] direction = new float[size];
        if (size < period + 1) {
            for (int i = 0; i < size; i++) {
                values[i] = Float.NaN;
                direction[i] = Float.NaN;
            }
            return new float[][]{values, direction};
        }
        float[] atr = Atr.compute(high, low, close, size, period);
        for (int i = 0; i < period; i++) {
            values[i] = Float.NaN;
            direction[i] = Float.NaN;
        }
        float mid = (high[period] + low[period]) / 2f;
        float upperBand = mid + multiplier * atr[period];
        float lowerBand = mid - multiplier * atr[period];
        boolean bullish = close[period] > mid;
        values[period] = bullish ? lowerBand : upperBand;
        direction[period] = bullish ? 1f : -1f;
        for (int i = period + 1; i < size; i++) {
            mid = (high[i] + low[i]) / 2f;
            float newUpper = mid + multiplier * atr[i];
            float newLower = mid - multiplier * atr[i];
            lowerBand = newLower > lowerBand ? newLower : (close[i - 1] > lowerBand ? lowerBand : newLower);
            upperBand = newUpper < upperBand ? newUpper : (close[i - 1] < upperBand ? upperBand : newUpper);
            if (bullish && close[i] < lowerBand) {
                bullish = false;
            } else if (!bullish && close[i] > upperBand) {
                bullish = true;
            }
            values[i] = bullish ? lowerBand : upperBand;
            direction[i] = bullish ? 1f : -1f;
        }
        return new float[][]{values, direction};
    }

}
