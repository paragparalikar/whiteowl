package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class BollingerBandsIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Bollinger Bands";
    private static final String PERIOD_SETTING = "Period";
    private static final String STD_DEV_SETTING = "Std Dev";
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_STD_DEV = 2.0;
    private static final Color BAND_COLOR = Color.web("#888888");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD),
                new IndicatorSetting(STD_DEV_SETTING, Double.class, DEFAULT_STD_DEV)
        );
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        double stdDevMult = resolveDouble(settings, STD_DEV_SETTING, DEFAULT_STD_DEV);
        int size = bars.size();
        double[] middle = new double[size];
        double[] upper = new double[size];
        double[] lower = new double[size];
        initializeEmpty(middle, Math.min(period - 1, size));
        initializeEmpty(upper, Math.min(period - 1, size));
        initializeEmpty(lower, Math.min(period - 1, size));
        if (size >= period) {
            computeBands(bars, middle, upper, lower, period, stdDevMult, size);
        }
        String label = INDICATOR_NAME + "(" + period + ", " + stdDevMult + ")";
        List<IndicatorResult.ExtraSeries> extras = List.of(
                new IndicatorResult.ExtraSeries(upper, BAND_COLOR),
                new IndicatorResult.ExtraSeries(lower, BAND_COLOR)
        );
        return new IndicatorResult(middle, BAND_COLOR, label, false, null, extras);
    }

    private void computeBands(Bars bars, double[] middle, double[] upper, double[] lower,
                              int period, double stdDevMult, int size) {
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += bars.getClose(i);
        }
        for (int i = period - 1; i < size; i++) {
            if (i > period - 1) {
                sum += bars.getClose(i) - bars.getClose(i - period);
            }
            double mean = sum / period;
            middle[i] = mean;
            double sqSum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = bars.getClose(j) - mean;
                sqSum += diff * diff;
            }
            double stdDev = Math.sqrt(sqSum / period);
            upper[i] = mean + stdDevMult * stdDev;
            lower[i] = mean - stdDevMult * stdDev;
        }
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
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
