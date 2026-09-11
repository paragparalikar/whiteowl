package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.scripting.indicator.script.GroovyIndicator;
import com.whiteowl.scripting.script.ScriptInput;
import com.whiteowl.scripting.script.ScriptInputDiscoverer;
import com.whiteowl.scripting.script.ScriptRepository;
import com.whiteowl.scripting.script.ScriptDescriptor;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ScriptedOverlayIndicator implements OverlayIndicator {

    private final GroovyIndicator groovyIndicator;
    private final ScriptDescriptor descriptor;
    private final ScriptRepository repository;
    private final boolean volumeOverlay;
    private List<ScriptInput> cachedInputs;
    private long cachedInputsTimestamp;

    public ScriptedOverlayIndicator(GroovyIndicator groovyIndicator,
                                    ScriptDescriptor descriptor, ScriptRepository repository,
                                    boolean volumeOverlay) {
        this.groovyIndicator = groovyIndicator;
        this.descriptor = descriptor;
        this.repository = repository;
        this.volumeOverlay = volumeOverlay;
    }

    @Override
    public String getName() {
        return groovyIndicator.getName();
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        List<ScriptInput> inputs = discoverInputs();
        return inputs.stream()
                .map(i -> new IndicatorSetting(i.getName(),
                        i.getDefaultValue() instanceof Integer ? Integer.class : Double.class,
                        i.getDefaultValue()))
                .toList();
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        try {
            Map<String, Number> overrides = toNumberMap(settings);
            groovyIndicator.setInputOverrides(overrides);
            float[] values = groovyIndicator.compute(bars.arrays());
            double[] doubles = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                doubles[i] = values[i];
            }
            String label = buildLabel(settings);
            return new IndicatorResult(doubles, Color.WHITE, label, volumeOverlay);
        } catch (Exception e) {
            log.warn("Scripted indicator '{}' failed: {}", groovyIndicator.getName(), e.getMessage());
            double[] empty = new double[bars.size()];
            Arrays.fill(empty, Double.NaN);
            return new IndicatorResult(empty, Color.WHITE, groovyIndicator.getName(), volumeOverlay);
        }
    }

    private String buildLabel(Map<String, Object> settings) {
        if (settings.isEmpty()) return groovyIndicator.getName();
        StringBuilder sb = new StringBuilder(groovyIndicator.getName()).append("(");
        boolean first = true;
        for (Object value : settings.values()) {
            if (!first) sb.append(", ");
            sb.append(value);
            first = false;
        }
        return sb.append(")").toString();
    }

    private static Map<String, Number> toNumberMap(Map<String, Object> settings) {
        Map<String, Number> overrides = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            if (entry.getValue() instanceof Number num) {
                overrides.put(entry.getKey(), num);
            }
        }
        return overrides;
    }

    private List<ScriptInput> discoverInputs() {
        try {
            long fileTimestamp = Files.getLastModifiedTime(descriptor.getScriptPath()).toMillis();
            if (cachedInputs != null && fileTimestamp == cachedInputsTimestamp) {
                return cachedInputs;
            }
            cachedInputs = ScriptInputDiscoverer.discoverIndicatorInputs(descriptor, repository);
            cachedInputsTimestamp = fileTimestamp;
            return cachedInputs;
        } catch (Exception e) {
            log.debug("Failed to discover inputs for '{}': {}", descriptor.getName(), e.getMessage());
            return List.of();
        }
    }

}
