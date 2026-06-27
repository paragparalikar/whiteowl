package com.whiteowl.core.indicator.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IndicatorScriptConstants {

    public static final String INDICATORS_DIR = "indicators";

    public static final String DEFAULT_TEMPLATE = """
            def period = input("Period", 20, 1, 500)
            def result = sma(close, period.intValue())
            return result
            """;

    public static final String SAMPLE_EMA_CROSSOVER = "EMA Crossover";
    public static final String SAMPLE_EMA_CROSSOVER_TEMPLATE = """
            def fastPeriod = input("Fast Period", 12, 1, 200)
            def slowPeriod = input("Slow Period", 26, 1, 500)
            def fast = ema(close, fastPeriod.intValue())
            def slow = ema(close, slowPeriod.intValue())
            def result = new float[barCount]
            for (int i = 0; i < barCount; i++) {
                result[i] = fast[i] - slow[i]
            }
            return result
            """;

    public static final String SAMPLE_RSI = "RSI 14";
    public static final String SAMPLE_RSI_TEMPLATE = """
            def period = input("Period", 14, 1, 200)
            return rsi(close, period.intValue())
            """;

    public static final String SAMPLE_BOLLINGER_WIDTH = "Bollinger Width";
    public static final String SAMPLE_BOLLINGER_WIDTH_TEMPLATE = """
            def period = input("Period", 20, 1, 500)
            def ma = sma(close, period.intValue())
            def result = new float[barCount]
            for (int i = period - 1; i < barCount; i++) {
                float sum = 0
                for (int j = i - period + 1; j <= i; j++) {
                    float diff = close[j] - ma[i]
                    sum += diff * diff
                }
                float stdDev = (float) Math.sqrt(sum / period)
                result[i] = ma[i] != 0 ? (2 * stdDev) / ma[i] : 0
            }
            return result
            """;

    public static final String[][] SAMPLES = {
            { SAMPLE_EMA_CROSSOVER, SAMPLE_EMA_CROSSOVER_TEMPLATE },
            { SAMPLE_RSI, SAMPLE_RSI_TEMPLATE },
            { SAMPLE_BOLLINGER_WIDTH, SAMPLE_BOLLINGER_WIDTH_TEMPLATE }
    };

}
