package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class ObvIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "OBV";

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of();
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int size = bars.size();
        double[] values = new double[size];
        if (size == 0) return buildResult(values);
        values[0] = bars.getVolume(0);
        for (int i = 1; i < size; i++) {
            float close = bars.getClose(i);
            float prevClose = bars.getClose(i - 1);
            if (close > prevClose) {
                values[i] = values[i - 1] + bars.getVolume(i);
            } else if (close < prevClose) {
                values[i] = values[i - 1] - bars.getVolume(i);
            } else {
                values[i] = values[i - 1];
            }
        }
        return buildResult(values);
    }

    private SubChartResult buildResult(double[] values) {
        return new SubChartResult(values, Color.web("#4caf50"), INDICATOR_NAME, true,
                List.of(), null, null, null);
    }

}
