package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class ActiveSubChartIndicator {

    private final SubChartIndicator indicator;
    private final Map<String, Object> settings;
    private final Color color;

    public ActiveSubChartIndicator(SubChartIndicator indicator, Color color) {
        this.indicator = indicator;
        this.color = color;
        this.settings = new LinkedHashMap<>();
        for (IndicatorSetting s : indicator.getSettings()) {
            settings.put(s.getName(), s.getDefaultValue());
        }
    }

    public SubChartResult compute(Bars bars) {
        return indicator.compute(bars, settings);
    }

    public void updateSetting(String name, Object value) {
        settings.put(name, value);
    }

    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder(indicator.getName());
        sb.append("(");
        boolean first = true;
        for (Object val : settings.values()) {
            if (!first) sb.append(", ");
            sb.append(val);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

}
