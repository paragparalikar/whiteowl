package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class KeltnerChannelsIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Keltner Channels";
    private static final String EMA_PERIOD_SETTING = "EMA Period";
    private static final String ATR_PERIOD_SETTING = "ATR Period";
    private static final String MULTIPLIER_SETTING = "Multiplier";
    private static final int DEFAULT_EMA_PERIOD = 20;
    private static final int DEFAULT_ATR_PERIOD = 10;
    private static final double DEFAULT_MULTIPLIER = 1.5;
    private static final Color MIDDLE_COLOR = Color.web("#2196f3");
    private static final Color BAND_COLOR = Color.web("#2196f380");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(EMA_PERIOD_SETTING, Integer.class, DEFAULT_EMA_PERIOD),
                new IndicatorSetting(ATR_PERIOD_SETTING, Integer.class, DEFAULT_ATR_PERIOD),
                new IndicatorSetting(MULTIPLIER_SETTING, Double.class, DEFAULT_MULTIPLIER)
        );
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int emaPeriod = resolveInt(settings, EMA_PERIOD_SETTING, DEFAULT_EMA_PERIOD);
        int atrPeriod = resolveInt(settings, ATR_PERIOD_SETTING, DEFAULT_ATR_PERIOD);
        double multiplier = resolveDouble(settings, MULTIPLIER_SETTING, DEFAULT_MULTIPLIER);
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[][] result = IndicatorFunctions.keltnerChannels(
                a.high(), a.low(), a.close(),
                size, emaPeriod, atrPeriod, (float) multiplier);
        double[] middle = toDouble(result[0], size);
        double[] upper = toDouble(result[1], size);
        double[] lower = toDouble(result[2], size);
        String label = INDICATOR_NAME + "(" + emaPeriod + ", " + atrPeriod + ", " + multiplier + ")";
        List<IndicatorResult.ExtraSeries> extras = List.of(
                new IndicatorResult.ExtraSeries(upper, BAND_COLOR),
                new IndicatorResult.ExtraSeries(lower, BAND_COLOR)
        );
        return new IndicatorResult(middle, MIDDLE_COLOR, label, false, null, extras);
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

    private double resolveDouble(Map<String, Object> settings, String key, double defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Double) return (Double) val;
        return defaultValue;
    }

}
