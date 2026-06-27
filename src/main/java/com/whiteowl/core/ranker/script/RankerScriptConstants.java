package com.whiteowl.core.ranker.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RankerScriptConstants {

    public static final String RANKERS_DIR = "rankers";

    public static final String DEFAULT_TEMPLATE = """
            def period = 14
            def values = rsi(close, period)
            if (barCount < period + 1) return null
            return (double) values[barCount - 1]
            """;

    public static final String SAMPLE_RSI_RANK = "RSI Rank";
    public static final String SAMPLE_RSI_RANK_TEMPLATE = """
            def period = 14
            if (barCount < period + 1) return null
            def values = rsi(close, period)
            return (double) values[barCount - 1]
            """;

    public static final String SAMPLE_MOMENTUM = "Momentum 20";
    public static final String SAMPLE_MOMENTUM_TEMPLATE = """
            def period = 20
            if (barCount < period + 1) return null
            int last = barCount - 1
            float prev = close[last - period]
            if (prev == 0) return null
            return (double) ((close[last] - prev) / prev * 100)
            """;

    public static final String SAMPLE_ATR_PERCENT = "ATR Percent";
    public static final String SAMPLE_ATR_PERCENT_TEMPLATE = """
            def period = 14
            if (barCount < period + 1) return null
            def atrValues = atr(period)
            int last = barCount - 1
            if (close[last] == 0) return null
            return (double) (atrValues[last] / close[last] * 100)
            """;

    public static final String[][] SAMPLES = {
            { SAMPLE_RSI_RANK, SAMPLE_RSI_RANK_TEMPLATE },
            { SAMPLE_MOMENTUM, SAMPLE_MOMENTUM_TEMPLATE },
            { SAMPLE_ATR_PERCENT, SAMPLE_ATR_PERCENT_TEMPLATE }
    };

}
