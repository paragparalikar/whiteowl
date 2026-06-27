package com.whiteowl.core.script;

import com.whiteowl.core.indicator.script.IndicatorDsl;
import com.whiteowl.core.screener.script.ScreenerDsl;
import groovy.lang.Script;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScriptInputDiscoverer {

    public static List<ScriptInput> discoverIndicatorInputs(String scriptSource)
            throws ScriptCompilationException {
        return discoverInputs(scriptSource, IndicatorDsl.class);
    }

    public static List<ScriptInput> discoverScreenerInputs(String scriptSource)
            throws ScriptCompilationException {
        return discoverInputs(scriptSource, ScreenerDsl.class);
    }

    public static List<ScriptInput> discoverIndicatorInputs(ScriptDescriptor descriptor,
                                                             ScriptRepository repository)
            throws ScriptCompilationException, IOException {
        Class<? extends Script> clazz = repository.getClassCache()
                .getOrCompile(descriptor, repository, IndicatorDsl.class);
        return discoverFromClass(clazz);
    }

    public static List<ScriptInput> discoverScreenerInputs(ScriptDescriptor descriptor,
                                                             ScriptRepository repository)
            throws ScriptCompilationException, IOException {
        Class<? extends Script> clazz = repository.getClassCache()
                .getOrCompile(descriptor, repository, ScreenerDsl.class);
        return discoverFromClass(clazz);
    }

    private static <T extends Script> List<ScriptInput> discoverInputs(String source,
                                                                        Class<T> baseClass)
            throws ScriptCompilationException {
        Script compiled = ScriptCompiler.compile(source, baseClass);
        return runDiscovery(compiled);
    }

    private static List<ScriptInput> discoverFromClass(Class<? extends Script> clazz) {
        Script compiled = ScriptCompiler.instantiate(clazz);
        return runDiscovery(compiled);
    }

    private static List<ScriptInput> runDiscovery(Script compiled) {
        if (compiled instanceof IndicatorDsl dsl) {
            dsl.setDiscoveryMode(true);
            dsl.clearInputs();
            try { compiled.run(); } catch (Exception e) {
                log.debug("Discovery run terminated: {}", e.getMessage());
            }
            return List.copyOf(dsl.getDeclaredInputs());
        }
        if (compiled instanceof ScreenerDsl dsl) {
            dsl.setDiscoveryMode(true);
            dsl.clearInputs();
            try { compiled.run(); } catch (Exception e) {
                log.debug("Discovery run terminated: {}", e.getMessage());
            }
            return List.copyOf(dsl.getDeclaredInputs());
        }
        return List.of();
    }

}
