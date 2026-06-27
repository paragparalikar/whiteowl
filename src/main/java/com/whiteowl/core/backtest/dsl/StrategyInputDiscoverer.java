package com.whiteowl.core.backtest.dsl;

import com.whiteowl.core.backtest.model.StrategyInput;
import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptCompiler;
import groovy.lang.Script;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyInputDiscoverer {

    public static List<StrategyInput> discover(String scriptSource) throws ScriptCompilationException {
        Script compiled = ScriptCompiler.compile(scriptSource, StrategyDsl.class);
        if (!(compiled instanceof StrategyDsl dsl)) {
            return List.of();
        }
        dsl.setDiscoveryMode(true);
        dsl.clearInputs();
        try {
            compiled.run();
        } catch (Exception ignored) {
        }
        return List.copyOf(dsl.getDeclaredInputs());
    }

}
