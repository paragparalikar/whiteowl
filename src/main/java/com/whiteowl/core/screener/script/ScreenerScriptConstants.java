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

}
