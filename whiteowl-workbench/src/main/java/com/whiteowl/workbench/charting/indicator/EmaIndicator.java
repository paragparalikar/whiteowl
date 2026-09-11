package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class EmaIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "EMA";
    private static final String PERIOD_SETTING = "Period";
    private static final String SOURCE_SETTING = "Source";
    private static final int DEFAULT_PERIOD = 20;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD),
                new IndicatorSetting(SOURCE_SETTING, PriceSource.class, PriceSource.CLOSE));
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        PriceSource source = resolveSource(settings);
        int size = bars.size();
        double[] values = new double[size];
        if (size < period) {
            initializeEmpty(values, size);
        } else {
            initializeEmpty(values, period - 1);
            double sum = 0;
            for (int i = 0; i < period; i++) {
                sum += source.resolve(bars, i);
            }
            values[period - 1] = sum / period;
            double multiplier = 2.0 / (period + 1);
            for (int i = period; i < size; i++) {
                values[i] = (source.resolve(bars, i) - values[i - 1]) * multiplier + values[i - 1];
            }
        }
        String label = INDICATOR_NAME + "(" + period + ", " + source.getLabel() + ")";
        boolean volumeOverlay = source == PriceSource.VOLUME;
        return new IndicatorResult(values, Color.WHITE, label, volumeOverlay);
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer) return (Integer) val;
        return defaultValue;
    }

    private PriceSource resolveSource(Map<String, Object> settings) {
        Object val = settings.get(SOURCE_SETTING);
        if (val instanceof PriceSource) return (PriceSource) val;
        return PriceSource.CLOSE;
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
    }

}
