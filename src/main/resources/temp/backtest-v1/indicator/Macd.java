package com.whiteowl.core.backtest.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Macd {

    private static final int LINE_INDEX = 0;
    private static final int SIGNAL_INDEX = 1;
    private static final int HISTOGRAM_INDEX = 2;
    private static final int RESULT_ARRAY_COUNT = 3;

    public static float[][] compute(float[] source, int size, int fastPeriod, int slowPeriod, int signalPeriod) {
        float[] fastEma = Ema.compute(source, size, fastPeriod);
        float[] slowEma = Ema.compute(source, size, slowPeriod);
        float[] macdLine = new float[size];
        for (int i = 0; i < size; i++) {
            macdLine[i] = Float.isNaN(fastEma[i]) || Float.isNaN(slowEma[i])
                    ? Float.NaN
                    : fastEma[i] - slowEma[i];
        }
        int macdStart = slowPeriod - 1;
        float[] signalLine = computeEmaOnValid(macdLine, size, signalPeriod, macdStart);
        float[] histogram = new float[size];
        for (int i = 0; i < size; i++) {
            histogram[i] = Float.isNaN(macdLine[i]) || Float.isNaN(signalLine[i])
                    ? Float.NaN
                    : macdLine[i] - signalLine[i];
        }
        float[][] result = new float[RESULT_ARRAY_COUNT][];
        result[LINE_INDEX] = macdLine;
        result[SIGNAL_INDEX] = signalLine;
        result[HISTOGRAM_INDEX] = histogram;
        return result;
    }

    private static float[] computeEmaOnValid(float[] source, int size, int period, int validStart) {
        float[] result = new float[size];
        int seedEnd = validStart + period;
        for (int i = 0; i < seedEnd && i < size; i++) {
            result[i] = Float.NaN;
        }
        if (seedEnd > size) return result;
        float sum = 0f;
        for (int i = validStart; i < seedEnd; i++) {
            sum += source[i];
        }
        float multiplier = 2.0f / (period + 1);
        result[seedEnd - 1] = sum / period;
        for (int i = seedEnd; i < size; i++) {
            result[i] = (source[i] - result[i - 1]) * multiplier + result[i - 1];
        }
        return result;
    }

}
