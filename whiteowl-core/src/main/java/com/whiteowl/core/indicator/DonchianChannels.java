package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DonchianChannels {

    public static float[][] compute(float[] high, float[] low, int size, int period) {
        float[] upper = Highest.compute(high, size, period);
        float[] lower = Lowest.compute(low, size, period);
        float[] middle = new float[size];
        for (int i = 0; i < size; i++) {
            if (Float.isNaN(upper[i]) || Float.isNaN(lower[i])) {
                middle[i] = Float.NaN;
            } else {
                middle[i] = (upper[i] + lower[i]) / 2f;
            }
        }
        return new float[][]{middle, upper, lower};
    }

}
