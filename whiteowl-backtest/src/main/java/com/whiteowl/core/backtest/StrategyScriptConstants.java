package com.whiteowl.core.backtest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyScriptConstants {

    public static final String STRATEGIES_DIR = "strategies";

    public static final String DEFAULT_TEMPLATE = """
            def fastPeriod = input("Fast Period", 20)
            def slowPeriod = input("Slow Period", 50)
            def fast = sma(close, fastPeriod)
            def slow = sma(close, slowPeriod)
            def cross = crossover(fast, slow)
            def crossDn = crossunder(fast, slow)
            
            for (int i = slowPeriod; i < barCount; i++) {
                if (cross[i]) longEntry(i)
                if (crossDn[i]) longExit(i)
            }
            """;

    public static final List<String> V2_ADDITIONAL_IMPORTS = List.of(
            "com.whiteowl.core.backtest.v2.engine.FillTiming",
            "com.whiteowl.core.backtest.v2.engine.FillPrice",
            "com.whiteowl.core.backtest.v2.engine.Side"
    );

}
