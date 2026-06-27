package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class StochRsiIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "StochRSI";
    private static final String RSI_PERIOD_SETTING = "RSI Period";
    private static final String STOCH_PERIOD_SETTING = "Stoch Period";
    private static final String K_SMOOTH_SETTING = "K Smooth";
    private static final String D_SMOOTH_SETTING = "D Smooth";
    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final int DEFAULT_STOCH_PERIOD = 14;
    private static final int DEFAULT_K_SMOOTH = 3;
    private static final int DEFAULT_D_SMOOTH = 3;
    private static final double OVERBOUGHT = 80;
    private static final double OVERSOLD = 20;
    private static final double MIN = 0;
    private static final double MAX = 100;
    private static final Color K_COLOR = Color.web("#2196f3");
    private static final Color D_COLOR = Color.web("#ff9800");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(RSI_PERIOD_SETTING, Integer.class, DEFAULT_RSI_PERIOD),
                new IndicatorSetting(STOCH_PERIOD_SETTING, Integer.class, DEFAULT_STOCH_PERIOD),
                new IndicatorSetting(K_SMOOTH_SETTING, Integer.class, DEFAULT_K_SMOOTH),
                new IndicatorSetting(D_SMOOTH_SETTING, Integer.class, DEFAULT_D_SMOOTH)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int rsiPeriod = resolveInt(settings, RSI_PERIOD_SETTING, DEFAULT_RSI_PERIOD);
        int stochPeriod = resolveInt(settings, STOCH_PERIOD_SETTING, DEFAULT_STOCH_PERIOD);
        int kSmooth = resolveInt(settings, K_SMOOTH_SETTING, DEFAULT_K_SMOOTH);
        int dSmooth = resolveInt(settings, D_SMOOTH_SETTING, DEFAULT_D_SMOOTH);
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[][] result = IndicatorFunctions.stochRsi(a.close(), size,
                rsiPeriod, stochPeriod, kSmooth, dSmooth);
        double[] kValues = toDouble(result[0], size);
        double[] dValues = toDouble(result[1], size);
        String label = INDICATOR_NAME + "(" + rsiPeriod + ", " + stochPeriod + ", " + kSmooth + ", " + dSmooth + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(dValues, D_COLOR)
        );
        return new SubChartResult(kValues, K_COLOR, label, MIN, MAX, extras,
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
