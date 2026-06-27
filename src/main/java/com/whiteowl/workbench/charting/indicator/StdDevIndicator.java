package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class StdDevIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "StdDev";
    private static final String PERIOD_SETTING = "Period";
    private static final String SOURCE_SETTING = "Source";
    private static final int DEFAULT_PERIOD = 20;
    private static final Color STDDEV_COLOR = Color.web("#e040fb");

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
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        PriceSource source = resolveSource(settings);
        int size = bars.size();
        double[] values = new double[size];
        if (size < period) {
            initializeEmpty(values, size);
        } else {
            initializeEmpty(values, period - 1);
            for (int i = period - 1; i < size; i++) {
                double sum = 0;
                int from = i - period + 1;
                for (int j = from; j <= i; j++) {
                    sum += source.resolve(bars, j);
                }
                double mean = sum / period;
                double variance = 0;
                for (int j = from; j <= i; j++) {
                    double diff = source.resolve(bars, j) - mean;
                    variance += diff * diff;
                }
                values[i] = Math.sqrt(variance / period);
            }
        }
        String label = INDICATOR_NAME + "(" + period + ", " + source.getLabel() + ")";
        return new SubChartResult(values, STDDEV_COLOR, label, true, List.of(), null, null, null);
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
