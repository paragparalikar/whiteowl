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

}
