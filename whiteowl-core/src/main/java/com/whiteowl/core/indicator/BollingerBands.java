package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BollingerBands {

    public static float[][] compute(float[] source, int size, int period, float multiplier) {
        float[] middle = new float[size];
        float[] upper = new float[size];
        float[] lower = new float[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            middle[i] = Float.NaN;
            upper[i] = Float.NaN;
            lower[i] = Float.NaN;
        }
        if (period > size) return new float[][]{middle, upper, lower};
        float sum = 0f;
        for (int i = 0; i < period; i++) {
            sum += source[i];
        }
        for (int i = period - 1; i < size; i++) {
            if (i > period - 1) {
                sum += source[i] - source[i - period];
            }
            float mean = sum / period;
            middle[i] = mean;
            float sqSum = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float diff = source[j] - mean;
                sqSum += diff * diff;
            }
            float stdDev = (float) Math.sqrt(sqSum / period);
            upper[i] = mean + multiplier * stdDev;
            lower[i] = mean - multiplier * stdDev;
        }
        return new float[][]{middle, upper, lower};
    }

}
