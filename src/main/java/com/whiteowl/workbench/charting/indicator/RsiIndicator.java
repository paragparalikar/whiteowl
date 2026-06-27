package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class RsiIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "RSI";
    private static final String PERIOD_SETTING = "Period";
    private static final String OVERBOUGHT_SETTING = "Overbought";
    private static final String OVERSOLD_SETTING = "Oversold";
    private static final int DEFAULT_PERIOD = 14;
    private static final double RSI_MIN = 0;
    private static final double RSI_MAX = 100;
    private static final int DEFAULT_OVERSOLD = 30;
    private static final int DEFAULT_OVERBOUGHT = 70;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD),
                new IndicatorSetting(OVERBOUGHT_SETTING, Integer.class, DEFAULT_OVERBOUGHT),
                new IndicatorSetting(OVERSOLD_SETTING, Integer.class, DEFAULT_OVERSOLD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        int overbought = resolveInt(settings, OVERBOUGHT_SETTING, DEFAULT_OVERBOUGHT);
        int oversold = resolveInt(settings, OVERSOLD_SETTING, DEFAULT_OVERSOLD);
        int size = bars.size();
        double[] values = new double[size];
        initializeEmpty(values, Math.min(period, size));
        if (size > period) {
            computeRsi(bars, values, period, size);
        }
        String label = INDICATOR_NAME + "(" + period + ")";
        return new SubChartResult(values, Color.WHITE, label,
                RSI_MIN, RSI_MAX, oversold, overbought);
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

    private void computeRsi(Bars bars, double[] values, int period, int size) {
        double gainSum = 0;
        double lossSum = 0;
        for (int i = 1; i <= period; i++) {
            double change = bars.getClose(i) - bars.getClose(i - 1);
            if (change > 0) gainSum += change;
            else lossSum -= change;
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        values[period] = computeRsiValue(avgGain, avgLoss);
        for (int i = period + 1; i < size; i++) {
            double change = bars.getClose(i) - bars.getClose(i - 1);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? -change : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            values[i] = computeRsiValue(avgGain, avgLoss);
        }
    }

    private double computeRsiValue(double avgGain, double avgLoss) {
        if (avgLoss == 0) return RSI_MAX;
        double rs = avgGain / avgLoss;
        return RSI_MAX - RSI_MAX / (1 + rs);
    }

}
