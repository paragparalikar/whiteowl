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

}
