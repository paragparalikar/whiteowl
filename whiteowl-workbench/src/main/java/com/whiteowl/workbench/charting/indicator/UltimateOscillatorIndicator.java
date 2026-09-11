package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class UltimateOscillatorIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Ultimate Oscillator";
    private static final String PERIOD1_SETTING = "Period 1";
    private static final String PERIOD2_SETTING = "Period 2";
    private static final String PERIOD3_SETTING = "Period 3";
    private static final int DEFAULT_PERIOD1 = 7;
    private static final int DEFAULT_PERIOD2 = 14;
    private static final int DEFAULT_PERIOD3 = 28;
    private static final double OVERBOUGHT = 70;
    private static final double OVERSOLD = 30;
    private static final double MIN = 0;
    private static final double MAX = 100;
    private static final Color UO_COLOR = Color.web("#7e57c2");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD1_SETTING, Integer.class, DEFAULT_PERIOD1),
                new IndicatorSetting(PERIOD2_SETTING, Integer.class, DEFAULT_PERIOD2),
                new IndicatorSetting(PERIOD3_SETTING, Integer.class, DEFAULT_PERIOD3)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period1 = resolveInt(settings, PERIOD1_SETTING, DEFAULT_PERIOD1);
        int period2 = resolveInt(settings, PERIOD2_SETTING, DEFAULT_PERIOD2);
        int period3 = resolveInt(settings, PERIOD3_SETTING, DEFAULT_PERIOD3);
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[] raw = IndicatorFunctions.ultimateOscillator(
                a.high(), a.low(), a.close(),
                size, period1, period2, period3);
        double[] values = toDouble(raw, size);
        String label = INDICATOR_NAME + "(" + period1 + ", " + period2 + ", " + period3 + ")";
        return new SubChartResult(values, UO_COLOR, label, MIN, MAX,
                OVERBOUGHT, OVERSOLD);
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
