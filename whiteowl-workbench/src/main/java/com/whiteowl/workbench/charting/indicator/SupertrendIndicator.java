package com.whiteowl.workbench.charting.indicator;

import static com.whiteowl.workbench.charting.ChartTheme.BEARISH;
import static com.whiteowl.workbench.charting.ChartTheme.BULLISH;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class SupertrendIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Supertrend";
    private static final String PERIOD_SETTING = "Period";
    private static final String MULTIPLIER_SETTING = "Multiplier";
    private static final int DEFAULT_PERIOD = 10;
    private static final double DEFAULT_MULTIPLIER = 3.0;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD),
                new IndicatorSetting(MULTIPLIER_SETTING, Double.class, DEFAULT_MULTIPLIER)
        );
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        double multiplier = resolveDouble(settings, MULTIPLIER_SETTING, DEFAULT_MULTIPLIER);
        int size = bars.size();
        double[] values = new double[size];
        Color[] barColors = new Color[size];
        if (size < period + 1) {
            initializeEmpty(values, size);
            return buildResult(values, barColors, period, multiplier);
        }
        double[] atr = computeAtr(bars, period, size);
        computeSupertrend(bars, atr, values, barColors, period, multiplier, size);
        return buildResult(values, barColors, period, multiplier);
    }

    private double[] computeAtr(Bars bars, int period, int size) {
        double[] tr = new double[size];
        tr[0] = bars.getHigh(0) - bars.getLow(0);
        for (int i = 1; i < size; i++) {
            double hl = bars.getHigh(i) - bars.getLow(i);
            double hc = Math.abs(bars.getHigh(i) - bars.getClose(i - 1));
            double lc = Math.abs(bars.getLow(i) - bars.getClose(i - 1));
            tr[i] = Math.max(hl, Math.max(hc, lc));
        }
        double[] atr = new double[size];
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += tr[i];
        }
        atr[period - 1] = sum / period;
        for (int i = period; i < size; i++) {
            atr[i] = (atr[i - 1] * (period - 1) + tr[i]) / period;
        }
        return atr;
    }

    private void computeSupertrend(Bars bars, double[] atr, double[] values,
                                   Color[] barColors, int period, double multiplier, int size) {
        initializeEmpty(values, period);
        double mid = (bars.getHigh(period) + bars.getLow(period)) / 2.0;
        double upperBand = mid + multiplier * atr[period];
        double lowerBand = mid - multiplier * atr[period];
        boolean bullish = bars.getClose(period) > mid;
        values[period] = bullish ? lowerBand : upperBand;
        barColors[period] = bullish ? BULLISH : BEARISH;
        for (int i = period + 1; i < size; i++) {
            mid = (bars.getHigh(i) + bars.getLow(i)) / 2.0;
            double newUpper = mid + multiplier * atr[i];
            double newLower = mid - multiplier * atr[i];
            lowerBand = newLower > lowerBand ? newLower : (bars.getClose(i - 1) > lowerBand ? lowerBand : newLower);
            upperBand = newUpper < upperBand ? newUpper : (bars.getClose(i - 1) < upperBand ? upperBand : newUpper);
            if (bullish && bars.getClose(i) < lowerBand) {
                bullish = false;
            } else if (!bullish && bars.getClose(i) > upperBand) {
                bullish = true;
            }
            values[i] = bullish ? lowerBand : upperBand;
            barColors[i] = bullish ? BULLISH : BEARISH;
        }
    }

    private IndicatorResult buildResult(double[] values, Color[] barColors, int period, double multiplier) {
        String label = INDICATOR_NAME + "(" + period + ", " + multiplier + ")";
        return new IndicatorResult(values, Color.WHITE, label, false, barColors);
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
