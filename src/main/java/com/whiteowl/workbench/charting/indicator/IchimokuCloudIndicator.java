package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class IchimokuCloudIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Ichimoku Cloud";
    private static final String TENKAN_SETTING = "Tenkan";
    private static final String KIJUN_SETTING = "Kijun";
    private static final String SENKOU_B_SETTING = "Senkou B";
    private static final String DISPLACEMENT_SETTING = "Displacement";
    private static final int DEFAULT_TENKAN = 9;
    private static final int DEFAULT_KIJUN = 26;
    private static final int DEFAULT_SENKOU_B = 52;
    private static final int DEFAULT_DISPLACEMENT = 26;
    private static final Color TENKAN_COLOR = Color.web("#2196f3");
    private static final Color KIJUN_COLOR = Color.web("#ef5350");
    private static final Color SENKOU_A_COLOR = Color.web("#26a69a");
    private static final Color SENKOU_B_COLOR = Color.web("#ff7043");
    private static final Color CHIKOU_COLOR = Color.web("#9c27b0");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(TENKAN_SETTING, Integer.class, DEFAULT_TENKAN),
                new IndicatorSetting(KIJUN_SETTING, Integer.class, DEFAULT_KIJUN),
                new IndicatorSetting(SENKOU_B_SETTING, Integer.class, DEFAULT_SENKOU_B),
                new IndicatorSetting(DISPLACEMENT_SETTING, Integer.class, DEFAULT_DISPLACEMENT)
        );
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int tenkan = resolveInt(settings, TENKAN_SETTING, DEFAULT_TENKAN);
        int kijun = resolveInt(settings, KIJUN_SETTING, DEFAULT_KIJUN);
        int senkouB = resolveInt(settings, SENKOU_B_SETTING, DEFAULT_SENKOU_B);
        int displacement = resolveInt(settings, DISPLACEMENT_SETTING, DEFAULT_DISPLACEMENT);
        int size = bars.size();
        double[] tenkanLine = midLine(bars, tenkan, size);
        double[] kijunLine = midLine(bars, kijun, size);
        double[] senkouALine = new double[size];
        double[] senkouBLine = new double[size];
        double[] chikouLine = new double[size];
        initializeEmpty(senkouALine, size);
        initializeEmpty(senkouBLine, size);
        initializeEmpty(chikouLine, size);
        double[] midLong = midLine(bars, senkouB, size);
        for (int i = 0; i < size; i++) {
            if (!Double.isNaN(tenkanLine[i]) && !Double.isNaN(kijunLine[i])) {
                int shifted = i + displacement;
                if (shifted < size) {
                    senkouALine[shifted] = (tenkanLine[i] + kijunLine[i]) / 2.0;
                }
            }
            if (!Double.isNaN(midLong[i])) {
                int shifted = i + displacement;
                if (shifted < size) {
                    senkouBLine[shifted] = midLong[i];
                }
            }
        }
        for (int i = displacement; i < size; i++) {
            chikouLine[i - displacement] = bars.getClose(i);
        }
        String label = INDICATOR_NAME + "(" + tenkan + ", " + kijun + ", " + senkouB + ")";
        List<IndicatorResult.ExtraSeries> extras = List.of(
                new IndicatorResult.ExtraSeries(kijunLine, KIJUN_COLOR),
                new IndicatorResult.ExtraSeries(senkouALine, SENKOU_A_COLOR),
                new IndicatorResult.ExtraSeries(senkouBLine, SENKOU_B_COLOR),
                new IndicatorResult.ExtraSeries(chikouLine, CHIKOU_COLOR)
        );
        return new IndicatorResult(tenkanLine, TENKAN_COLOR, label, false, null, extras);
    }

    private double[] midLine(Bars bars, int period, int size) {
        double[] result = new double[size];
        initializeEmpty(result, Math.min(period - 1, size));
        for (int i = period - 1; i < size; i++) {
            float highest = Float.MIN_VALUE;
            float lowest = Float.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                if (bars.getHigh(j) > highest) highest = bars.getHigh(j);
                if (bars.getLow(j) < lowest) lowest = bars.getLow(j);
            }
            result[i] = (highest + lowest) / 2.0;
        }
        return result;
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
