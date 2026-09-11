package com.whiteowl.workbench.charting.indicator;

import static com.whiteowl.workbench.charting.ChartTheme.BEARISH;
import static com.whiteowl.workbench.charting.ChartTheme.BULLISH;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class MacdIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "MACD";
    private static final String FAST_SETTING = "Fast";
    private static final String SLOW_SETTING = "Slow";
    private static final String SIGNAL_SETTING = "Signal";
    private static final int DEFAULT_FAST = 12;
    private static final int DEFAULT_SLOW = 26;
    private static final int DEFAULT_SIGNAL = 9;
    private static final Color MACD_COLOR = Color.web("#2196f3");
    private static final Color SIGNAL_COLOR = Color.web("#ff9800");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(FAST_SETTING, Integer.class, DEFAULT_FAST),
                new IndicatorSetting(SLOW_SETTING, Integer.class, DEFAULT_SLOW),
                new IndicatorSetting(SIGNAL_SETTING, Integer.class, DEFAULT_SIGNAL)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int fast = resolveInt(settings, FAST_SETTING, DEFAULT_FAST);
        int slow = resolveInt(settings, SLOW_SETTING, DEFAULT_SLOW);
        int signal = resolveInt(settings, SIGNAL_SETTING, DEFAULT_SIGNAL);
        int size = bars.size();
        double[] fastEma = computeEma(bars, fast, size);
        double[] slowEma = computeEma(bars, slow, size);
        double[] macdLine = new double[size];
        for (int i = 0; i < size; i++) {
            if (Double.isNaN(fastEma[i]) || Double.isNaN(slowEma[i])) {
                macdLine[i] = Double.NaN;
            } else {
                macdLine[i] = fastEma[i] - slowEma[i];
            }
        }
        double[] signalLine = computeEmaFromValues(macdLine, signal, size);
        double[] histogram = new double[size];
        for (int i = 0; i < size; i++) {
            if (Double.isNaN(macdLine[i]) || Double.isNaN(signalLine[i])) {
                histogram[i] = Double.NaN;
            } else {
                histogram[i] = macdLine[i] - signalLine[i];
            }
        }
        String label = INDICATOR_NAME + "(" + fast + ", " + slow + ", " + signal + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(signalLine, SIGNAL_COLOR)
        );
        return new SubChartResult(macdLine, MACD_COLOR, label, true, extras,
                histogram, BULLISH, BEARISH);
    }

    private double[] computeEma(Bars bars, int period, int size) {
        double[] ema = new double[size];
        if (size < period) {
            initializeEmpty(ema, size);
            return ema;
        }
        initializeEmpty(ema, period - 1);
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += bars.getClose(i);
        }
        ema[period - 1] = sum / period;
        double multiplier = 2.0 / (period + 1);
        for (int i = period; i < size; i++) {
            ema[i] = (bars.getClose(i) - ema[i - 1]) * multiplier + ema[i - 1];
        }
        return ema;
    }

    private double[] computeEmaFromValues(double[] data, int period, int size) {
        double[] ema = new double[size];
        int start = findFirstValid(data, size);
        if (start < 0 || start + period > size) {
            initializeEmpty(ema, size);
            return ema;
        }
        initializeEmpty(ema, start + period - 1);
        double sum = 0;
        for (int i = start; i < start + period; i++) {
            sum += data[i];
        }
        ema[start + period - 1] = sum / period;
        double multiplier = 2.0 / (period + 1);
        for (int i = start + period; i < size; i++) {
            if (Double.isNaN(data[i])) {
                ema[i] = ema[i - 1];
            } else {
                ema[i] = (data[i] - ema[i - 1]) * multiplier + ema[i - 1];
            }
        }
        return ema;
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
