package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class AroonIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Aroon";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 25;
    private static final double MIN = 0;
    private static final double MAX = 100;
    private static final Color AROON_UP_COLOR = Color.web("#26a69a");
    private static final Color AROON_DOWN_COLOR = Color.web("#ef5350");

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
        float[][] result = IndicatorFunctions.aroon(
                a.high(), a.low(), size, period);
        double[] aroonUp = toDouble(result[0], size);
        double[] aroonDown = toDouble(result[1], size);
        String label = INDICATOR_NAME + "(" + period + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(aroonDown, AROON_DOWN_COLOR)
        );
        return new SubChartResult(aroonUp, AROON_UP_COLOR, label, MIN, MAX, extras);
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
