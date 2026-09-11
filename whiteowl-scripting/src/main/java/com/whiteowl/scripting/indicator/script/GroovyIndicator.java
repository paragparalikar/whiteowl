package com.whiteowl.scripting.indicator.script;

import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.scripting.script.ScriptCallStack;
import com.whiteowl.scripting.script.ScriptCompilationException;
import com.whiteowl.scripting.script.ScriptCompiler;
import com.whiteowl.scripting.script.ScriptDescriptor;
import com.whiteowl.scripting.script.ScriptRepository;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public final class GroovyIndicator {

    private final ScriptDescriptor descriptor;
    private final ScriptRepository repository;
    private Map<String, Number> inputOverrides = Map.of();

    public GroovyIndicator(ScriptDescriptor descriptor, ScriptRepository repository) {
        this.descriptor = descriptor;
        this.repository = repository;
    }

    public void setInputOverrides(Map<String, Number> overrides) {
        this.inputOverrides = overrides != null ? overrides : Map.of();
    }

    public float[] compute(BarsArrays arrays) throws IOException, ScriptCompilationException {
        IndicatorDsl dsl = createInstance();
        dsl.setInputOverrides(inputOverrides);
        dsl.bind(arrays);
        ScriptCallStack.push(descriptor.getName());
        try {
            Object result = dsl.run();
            return toFloatArray(result);
        } finally {
            ScriptCallStack.pop();
        }
    }

    private IndicatorDsl createInstance() throws IOException, ScriptCompilationException {
        Class<? extends Script> clazz = repository.getClassCache()
                .getOrCompile(descriptor, repository, IndicatorDsl.class);
        Script instance = ScriptCompiler.instantiate(clazz);
        return (IndicatorDsl) instance;
    }

    public String getName() {
        return descriptor.getName();
    }

    public DisplayMode discoverDisplayMode() {
        try {
            IndicatorDsl dsl = createInstance();
            dsl.setDiscoveryMode(true);
            dsl.run();
            return dsl.getDisplayMode();
        } catch (Exception e) {
            log.debug("Failed to discover display mode for '{}': {}", descriptor.getName(), e.getMessage());
            return DisplayMode.SUBCHART;
        }
    }

    private static float[] toFloatArray(Object result) {
        if (result instanceof float[] fa) return fa;
        if (result instanceof double[] da) {
            float[] fa = new float[da.length];
            for (int i = 0; i < da.length; i++) {
                fa[i] = (float) da[i];
            }
            return fa;
        }
        if (result instanceof Number[] na) {
            float[] fa = new float[na.length];
            for (int i = 0; i < na.length; i++) {
                fa[i] = na[i].floatValue();
            }
            return fa;
        }
        throw new IllegalStateException("Indicator script must return a float[], double[], or Number[] array");
    }

}
