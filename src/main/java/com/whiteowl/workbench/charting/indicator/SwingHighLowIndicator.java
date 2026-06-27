package com.whiteowl.workbench.charting.indicator;

import static com.whiteowl.workbench.charting.ChartTheme.BEARISH;
import static com.whiteowl.workbench.charting.ChartTheme.BULLISH;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class SwingHighLowIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Swing H/L";
    private static final String STRENGTH_SETTING = "Strength";
    private static final int DEFAULT_STRENGTH = 5;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(new IndicatorSetting(STRENGTH_SETTING, Integer.class, DEFAULT_STRENGTH));
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int strength = resolveInt(settings, STRENGTH_SETTING, DEFAULT_STRENGTH);
        int size = bars.size();
        double[] values = new double[size];
        Color[] barColors = new Color[size];
        initializeEmpty(values, size);
        if (size >= strength * 2 + 1) {
            computeSwings(bars, values, barColors, strength, size);
        }
        String label = INDICATOR_NAME + "(" + strength + ")";
        return new IndicatorResult(values, Color.WHITE, label, false, barColors);
    }

    private void computeSwings(Bars bars, double[] values, Color[] barColors, int strength, int size) {
        float lastSwingHigh = Float.NaN;
        float lastSwingLow = Float.NaN;
        for (int i = strength; i < size - strength; i++) {
            if (isSwingHigh(bars, i, strength)) {
                lastSwingHigh = bars.getHigh(i);
                values[i] = lastSwingHigh;
                barColors[i] = BULLISH;
            } else if (isSwingLow(bars, i, strength)) {
                lastSwingLow = bars.getLow(i);
                values[i] = lastSwingLow;
                barColors[i] = BEARISH;
            }
        }
        fillForward(values, size);
    }

    private boolean isSwingHigh(Bars bars, int index, int strength) {
        float high = bars.getHigh(index);
        for (int i = 1; i <= strength; i++) {
            if (bars.getHigh(index - i) >= high || bars.getHigh(index + i) >= high) return false;
        }
        return true;
    }

    private boolean isSwingLow(Bars bars, int index, int strength) {
        float low = bars.getLow(index);
        for (int i = 1; i <= strength; i++) {
            if (bars.getLow(index - i) <= low || bars.getLow(index + i) <= low) return false;
        }
        return true;
    }

    private void fillForward(double[] values, int size) {
        double last = Double.NaN;
        for (int i = 0; i < size; i++) {
            if (!Double.isNaN(values[i])) {
                last = values[i];
            } else {
                values[i] = last;
            }
        }
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
