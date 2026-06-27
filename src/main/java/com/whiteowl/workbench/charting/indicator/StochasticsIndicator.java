package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class StochasticsIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Stochastics";
    private static final String K_PERIOD_SETTING = "%K Period";
    private static final String D_PERIOD_SETTING = "%D Period";
    private static final String OVERBOUGHT_SETTING = "Overbought";
    private static final String OVERSOLD_SETTING = "Oversold";
    private static final int DEFAULT_K_PERIOD = 14;
    private static final int DEFAULT_D_PERIOD = 3;
    private static final int DEFAULT_OVERBOUGHT = 80;
    private static final int DEFAULT_OVERSOLD = 20;
    private static final double STOCH_MIN = 0;
    private static final double STOCH_MAX = 100;
    private static final Color K_COLOR = Color.web("#2196f3");
    private static final Color D_COLOR = Color.web("#ff9800");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(K_PERIOD_SETTING, Integer.class, DEFAULT_K_PERIOD),
                new IndicatorSetting(D_PERIOD_SETTING, Integer.class, DEFAULT_D_PERIOD),
                new IndicatorSetting(OVERBOUGHT_SETTING, Integer.class, DEFAULT_OVERBOUGHT),
                new IndicatorSetting(OVERSOLD_SETTING, Integer.class, DEFAULT_OVERSOLD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int kPeriod = resolveInt(settings, K_PERIOD_SETTING, DEFAULT_K_PERIOD);
        int dPeriod = resolveInt(settings, D_PERIOD_SETTING, DEFAULT_D_PERIOD);
        int overbought = resolveInt(settings, OVERBOUGHT_SETTING, DEFAULT_OVERBOUGHT);
        int oversold = resolveInt(settings, OVERSOLD_SETTING, DEFAULT_OVERSOLD);
        int size = bars.size();
        double[] kValues = computeK(bars, kPeriod, size);
        double[] dValues = computeSma(kValues, dPeriod, size);
        String label = INDICATOR_NAME + "(" + kPeriod + ", " + dPeriod + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(dValues, D_COLOR)
        );
        return new SubChartResult(kValues, K_COLOR, label,
                STOCH_MIN, STOCH_MAX, extras, oversold, overbought);
    }

    private double[] computeK(Bars bars, int period, int size) {
        double[] k = new double[size];
        for (int i = 0; i < Math.min(period - 1, size); i++) {
            k[i] = Double.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                if (bars.getHigh(j) > highest) highest = bars.getHigh(j);
                if (bars.getLow(j) < lowest) lowest = bars.getLow(j);
            }
            double range = highest - lowest;
            k[i] = range > 0 ? ((bars.getClose(i) - lowest) / range) * STOCH_MAX : STOCH_MAX / 2;
        }
        return k;
    }

    private double[] computeSma(double[] data, int period, int size) {
        double[] sma = new double[size];
        int start = findFirstValid(data, size);
        if (start < 0 || start + period > size) {
            initializeEmpty(sma, size);
            return sma;
        }
        initializeEmpty(sma, start + period - 1);
        double sum = 0;
        for (int i = start; i < start + period; i++) {
            sum += data[i];
        }
        sma[start + period - 1] = sum / period;
        for (int i = start + period; i < size; i++) {
            sum += data[i] - data[i - period];
            sma[i] = sum / period;
        }
        return sma;
    }

    private int findFirstValid(double[] data, int size) {
        for (int i = 0; i < size; i++) {
            if (!Double.isNaN(data[i])) return i;
        }
        return -1;
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

}
