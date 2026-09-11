package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class AtrIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "ATR";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 14;
    private static final Color ATR_COLOR = Color.web("#2196f3");

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
        double[] values = new double[size];
        if (size < period + 1) {
            initializeEmpty(values, size);
        } else {
            initializeEmpty(values, period);
            double sum = 0;
            for (int i = 1; i <= period; i++) {
                sum += trueRange(bars, i);
            }
            values[period] = sum / period;
            for (int i = period + 1; i < size; i++) {
                values[i] = (values[i - 1] * (period - 1) + trueRange(bars, i)) / period;
            }
        }
        String label = INDICATOR_NAME + "(" + period + ")";
        return new SubChartResult(values, ATR_COLOR, label, true, List.of(), null, null, null);
    }

    private double trueRange(Bars bars, int i) {
        double hl = bars.getHigh(i) - bars.getLow(i);
        double hc = Math.abs(bars.getHigh(i) - bars.getClose(i - 1));
        double lc = Math.abs(bars.getLow(i) - bars.getClose(i - 1));
        return Math.max(hl, Math.max(hc, lc));
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
