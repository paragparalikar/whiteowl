package com.whiteowl.workbench.charting.indicator;

import static com.whiteowl.workbench.charting.ChartTheme.BEARISH;
import static com.whiteowl.workbench.charting.ChartTheme.BULLISH;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class ParabolicSarIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Parabolic SAR";
    private static final String AF_START_SETTING = "AF Start";
    private static final String AF_STEP_SETTING = "AF Step";
    private static final String AF_MAX_SETTING = "AF Max";
    private static final double DEFAULT_AF_START = 0.02;
    private static final double DEFAULT_AF_STEP = 0.02;
    private static final double DEFAULT_AF_MAX = 0.20;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(AF_START_SETTING, Double.class, DEFAULT_AF_START),
                new IndicatorSetting(AF_STEP_SETTING, Double.class, DEFAULT_AF_STEP),
                new IndicatorSetting(AF_MAX_SETTING, Double.class, DEFAULT_AF_MAX)
        );
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        double afStart = resolveDouble(settings, AF_START_SETTING, DEFAULT_AF_START);
        double afStep = resolveDouble(settings, AF_STEP_SETTING, DEFAULT_AF_STEP);
        double afMax = resolveDouble(settings, AF_MAX_SETTING, DEFAULT_AF_MAX);
        int size = bars.size();
        double[] values = new double[size];
        Color[] barColors = new Color[size];
        if (size < 2) {
            initializeEmpty(values, size);
            return buildResult(values, barColors, afStart, afStep, afMax);
        }
        computeSar(bars, values, barColors, size, afStart, afStep, afMax);
        return buildResult(values, barColors, afStart, afStep, afMax);
    }

    private void computeSar(Bars bars, double[] values, Color[] barColors, int size,
                            double afStart, double afStep, double afMax) {
        boolean bullish = bars.getClose(1) >= bars.getClose(0);
        double af = afStart;
        double ep = bullish ? bars.getHigh(0) : bars.getLow(0);
        values[0] = bullish ? bars.getLow(0) : bars.getHigh(0);
        barColors[0] = bullish ? BULLISH : BEARISH;
        for (int i = 1; i < size; i++) {
            double prevSar = values[i - 1];
            double newSar = prevSar + af * (ep - prevSar);
            if (bullish) {
                newSar = Math.min(newSar, Math.min(bars.getLow(i - 1),
                        i >= 2 ? bars.getLow(i - 2) : bars.getLow(i - 1)));
                if (bars.getLow(i) < newSar) {
                    bullish = false;
                    newSar = ep;
                    ep = bars.getLow(i);
                    af = afStart;
                } else {
                    if (bars.getHigh(i) > ep) {
                        ep = bars.getHigh(i);
                        af = Math.min(af + afStep, afMax);
                    }
                }
            } else {
                newSar = Math.max(newSar, Math.max(bars.getHigh(i - 1),
                        i >= 2 ? bars.getHigh(i - 2) : bars.getHigh(i - 1)));
                if (bars.getHigh(i) > newSar) {
                    bullish = true;
                    newSar = ep;
                    ep = bars.getHigh(i);
                    af = afStart;
                } else {
                    if (bars.getLow(i) < ep) {
                        ep = bars.getLow(i);
                        af = Math.min(af + afStep, afMax);
                    }
                }
            }
            values[i] = newSar;
            barColors[i] = bullish ? BULLISH : BEARISH;
        }
    }

    private IndicatorResult buildResult(double[] values, Color[] barColors,
                                        double afStart, double afStep, double afMax) {
        String label = INDICATOR_NAME + "(" + afStart + ", " + afStep + ", " + afMax + ")";
        return new IndicatorResult(values, Color.WHITE, label, false, barColors);
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
    }

    private double resolveDouble(Map<String, Object> settings, String key, double defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Double) return (Double) val;
        return defaultValue;
    }

}
