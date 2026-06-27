package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class DonchianChannelsIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Donchian Channels";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 20;
    private static final Color MIDDLE_COLOR = Color.web("#ff9800");
    private static final Color BAND_COLOR = Color.web("#ff980080");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[][] result = IndicatorFunctions.donchianChannels(
                a.high(), a.low(), size, period);
        double[] middle = toDouble(result[0], size);
        double[] upper = toDouble(result[1], size);
        double[] lower = toDouble(result[2], size);
        String label = INDICATOR_NAME + "(" + period + ")";
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

}
