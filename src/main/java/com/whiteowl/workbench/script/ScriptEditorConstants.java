package com.whiteowl.workbench.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScriptEditorConstants {

    public static final String DEFAULT_INDICATOR_NAME = "New Indicator";

    public static final String DEFAULT_SCREENER_NAME = "New Screener";

    public static final String DEFAULT_RANKER_NAME = "New Ranker";

    public static final String INDICATOR_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)

            Return: float[] (indicator values per bar)""";

    public static final String SCREENER_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)

            Return: boolean (true if scrip matches the screen)""";

    public static final String RANKER_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)

            Return: Number (rank score, higher = better)
            Return null to exclude the scrip from ranking""";

}
