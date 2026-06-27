package com.whiteowl.core.screener.script;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.script.ScriptCallStack;
import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptCompiler;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptInput;
import com.whiteowl.core.script.ScriptInputDiscoverer;
import com.whiteowl.core.script.ScriptRepository;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class GroovyScreen implements Screen {

    private final ScriptDescriptor descriptor;
    private final ScriptRepository repository;
    private final Map<String, Number> inputOverrides = new LinkedHashMap<>();
    private List<ScriptInput> cachedInputs;
    private long cachedInputsTimestamp;

    public GroovyScreen(ScriptDescriptor descriptor, ScriptRepository repository) {
        this.descriptor = descriptor;
        this.repository = repository;
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public List<ScreenSetting> getSettings() {
        List<ScriptInput> inputs = discoverInputs();
        return inputs.stream()
                .map(i -> new ScreenSetting(i.getName(),
                        i.getDefaultValue() instanceof Integer ? Integer.class : Double.class,
                        i.getDefaultValue()))
                .toList();
    }

    @Override
    public Map<String, Object> getSettingValues() {
        List<ScriptInput> inputs = discoverInputs();
        Map<String, Object> values = new LinkedHashMap<>();
        for (ScriptInput input : inputs) {
            values.put(input.getName(),
                    inputOverrides.getOrDefault(input.getName(), input.getDefaultValue()));
        }
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (value instanceof Number num) {
            inputOverrides.put(name, num);
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        try {
            ScreenerDsl dsl = createInstance();
            dsl.setInputOverrides(Collections.unmodifiableMap(inputOverrides));
            dsl.bind(bars);
            ScriptCallStack.push(descriptor.getName());
            try {
                Object result = dsl.run();
                return Boolean.TRUE.equals(result);
            } finally {
                ScriptCallStack.pop();
            }
        } catch (Exception e) {
            log.debug("Screener script '{}' failed for {}: {}", descriptor.getName(),
                    scrip.getSymbol(), e.getMessage());
            return false;
        }
    }

    private ScreenerDsl createInstance() throws IOException, ScriptCompilationException {
        Class<? extends Script> clazz = repository.getClassCache()
                .getOrCompile(descriptor, repository, ScreenerDsl.class);
        Script instance = ScriptCompiler.instantiate(clazz);
        return (ScreenerDsl) instance;
    }

    private List<ScriptInput> discoverInputs() {
        try {
            long fileTimestamp = Files.getLastModifiedTime(descriptor.getScriptPath()).toMillis();
            if (cachedInputs != null && fileTimestamp == cachedInputsTimestamp) {
                return cachedInputs;
            }
            cachedInputs = ScriptInputDiscoverer.discoverScreenerInputs(descriptor, repository);
            cachedInputsTimestamp = fileTimestamp;
            return cachedInputs;
        } catch (Exception e) {
            log.debug("Failed to discover inputs for '{}': {}", descriptor.getName(), e.getMessage());
            return List.of();
        }
    }

}
