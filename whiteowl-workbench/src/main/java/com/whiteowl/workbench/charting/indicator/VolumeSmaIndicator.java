package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class VolumeSmaIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Volume SMA";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 20;
    private static final Color VOLUME_SMA_COLOR = Color.web("#ff9800");

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
        double[] values = new double[size];
        if (size < period) {
            initializeEmpty(values, size);
        } else {
            initializeEmpty(values, period - 1);
            long sum = 0;
            for (int i = 0; i < period; i++) {
                sum += bars.getVolume(i);
            }
            values[period - 1] = (double) sum / period;
            for (int i = period; i < size; i++) {
                sum += bars.getVolume(i) - bars.getVolume(i - period);
                values[i] = (double) sum / period;
            }
        }
        String label = INDICATOR_NAME + "(" + period + ")";
        return new IndicatorResult(values, VOLUME_SMA_COLOR, label, true);
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer) return (Integer) val;
        return defaultValue;
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
    }

}
