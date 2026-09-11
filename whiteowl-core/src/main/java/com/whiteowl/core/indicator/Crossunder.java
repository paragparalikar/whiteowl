package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Crossunder {

    public static boolean[] compute(float[] a, float[] b, int size) {
        boolean[] result = new boolean[size];
        for (int i = 1; i < size; i++) {
            result[i] = a[i] < b[i] && a[i - 1] >= b[i - 1];
        }
        return result;
    }

}
