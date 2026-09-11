package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * Relative Volume (RVOL) — sub-chart histogram indicator.
 *
 * RVOL(i) = Volume(i) / SMA(Volume, period)(i)
 *
 * Displayed as a histogram: green bars for RVOL >= 1.0, red bars for RVOL < 1.0.
 * Reference line at 1.0 (average volume baseline).
 */
public final class RvolIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "RVOL";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 20;

    private static final Color RVOL_COLOR = Color.web("#42a5f5");
    private static final Color ABOVE_AVG_COLOR = Color.web("#4caf50");
    private static final Color BELOW_AVG_COLOR = Color.web("#ef5350");

    private static final double REFERENCE_LINE = 1.0;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        int size = bars.size();
        double[] values = new double[size];

        if (size < period) {
            initializeEmpty(values, size);
            return buildResult(values, period);
        }

        initializeEmpty(values, period - 1);

        // Rolling SMA of volume using long arithmetic
        long sum = 0;
        for (int i = 0; i < period; i++) {
            sum += bars.getVolume(i);
        }
        double avg = (double) sum / period;
        values[period - 1] = avg > 0 ? bars.getVolume(period - 1) / avg : Double.NaN;

        for (int i = period; i < size; i++) {
            sum += bars.getVolume(i) - bars.getVolume(i - period);
            avg = (double) sum / period;
            values[i] = avg > 0 ? bars.getVolume(i) / avg : Double.NaN;
        }

        return buildResult(values, period);
    }

    private SubChartResult buildResult(double[] values, int period) {
        String label = INDICATOR_NAME + "(" + period + ")";
        // Shift histogram so 1.0 becomes 0 — gives correct green/red coloring
        // from the existing renderer (positive = green, negative = red)
        double[] histogram = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            histogram[i] = Double.isNaN(values[i]) ? Double.NaN : values[i] - 1.0;
        }
        return new SubChartResult(values, RVOL_COLOR, label, true, List.of(),
                histogram, ABOVE_AVG_COLOR, BELOW_AVG_COLOR, REFERENCE_LINE);
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
