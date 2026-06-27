package com.whiteowl.core.screener.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScreenerScriptConstants {

    public static final String SCREENERS_DIR = "screeners";

    public static final String DEFAULT_TEMPLATE = """
            def period = input("Period", 200, 1, 500)
            if (barCount < period.intValue()) return false
            def ma = sma(close, period.intValue())
            return close[barCount - 1] > ma[barCount - 1]
            """;

    public static final String SAMPLE_ABOVE_50_SMA = "Above 50 SMA";
    public static final String SAMPLE_ABOVE_50_SMA_TEMPLATE = """
            def period = input("Period", 50, 1, 500)
            if (barCount < period.intValue()) return false
            def ma = sma(close, period.intValue())
            return close[barCount - 1] > ma[barCount - 1]
            """;

    public static final String SAMPLE_RSI_OVERSOLD = "RSI Oversold";
    public static final String SAMPLE_RSI_OVERSOLD_TEMPLATE = """
            def period = input("Period", 14, 1, 200)
            def threshold = input("Threshold", 30, 1, 100)
            if (barCount < period.intValue() + 1) return false
            def values = rsi(close, period.intValue())
            return values[barCount - 1] < threshold.doubleValue()
            """;

    public static final String SAMPLE_VOLUME_SPIKE = "Volume Spike";
    public static final String SAMPLE_VOLUME_SPIKE_TEMPLATE = """
            def period = input("Period", 20, 1, 200)
            def multiplier = input("Multiplier", 2.0, 1.0, 10.0)
            if (barCount < period.intValue()) return false
            float sum = 0
            int last = barCount - 1
            for (int i = last - period.intValue(); i < last; i++) {
                sum += volume[i]
            }
            float avgVolume = sum / period.intValue()
            return volume[last] > avgVolume * multiplier.floatValue()
            """;

    public static final String[][] SAMPLES = {
            { SAMPLE_ABOVE_50_SMA, SAMPLE_ABOVE_50_SMA_TEMPLATE },
            { SAMPLE_RSI_OVERSOLD, SAMPLE_RSI_OVERSOLD_TEMPLATE },
            { SAMPLE_VOLUME_SPIKE, SAMPLE_VOLUME_SPIKE_TEMPLATE }
    };

}
