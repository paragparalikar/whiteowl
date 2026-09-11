package com.whiteowl.core.backtest.v2.dsl;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.scripting.script.ScriptCompilationException;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyInputDiscoverer {

    public static List<StrategyInput> discover(String scriptSource) throws ScriptCompilationException {
        Script compiled = ScriptCompiler.compile(scriptSource, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
        if (!(compiled instanceof TradingStrategyBase strategy)) {
            return List.of();
        }
        strategy.setDiscoveryMode(true);
        strategy.clearInputs();
        try {
            compiled.run();
        } catch (Exception ignored) {
        }
        return List.copyOf(strategy.getDeclaredInputs());
    }

}
