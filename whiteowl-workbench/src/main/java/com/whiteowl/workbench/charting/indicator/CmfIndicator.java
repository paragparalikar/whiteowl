package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class CmfIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "CMF";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 20;
    private static final double ZERO_LINE = 0;
    private static final Color CMF_COLOR = Color.web("#66bb6a");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[] raw = IndicatorFunctions.cmf(
                a.high(), a.low(), a.close(),
                a.volume(), size, period);
        double[] values = toDouble(raw, size);
        String label = INDICATOR_NAME + "(" + period + ")";
        return new SubChartResult(values, CMF_COLOR, label, true, List.of(),
                null, null, null, ZERO_LINE);
    }

    private double[] toDouble(float[] src, int size) {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = Float.isNaN(src[i]) ? Double.NaN : src[i];
        }
        return result;
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer) return (Integer) val;
        return defaultValue;
    }

}
