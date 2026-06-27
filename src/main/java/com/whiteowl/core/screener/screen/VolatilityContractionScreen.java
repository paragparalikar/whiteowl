package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VolatilityContractionScreen implements Screen {

    private static final String NAME = "Volatility Contraction";
    private static final String SETTING_BAR_COUNT = "Bar Count";
    private static final String SETTING_MAX_STOCHASTICS_K = "Max Stochastics %K";
    private static final int DEFAULT_BAR_COUNT = 13;
    private static final double DEFAULT_MAX_STOCHASTICS_K = 10.0;

    private int barCount = DEFAULT_BAR_COUNT;
    private double maxStochasticsK = DEFAULT_MAX_STOCHASTICS_K;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_BAR_COUNT, Integer.class, DEFAULT_BAR_COUNT),
                new ScreenSetting(SETTING_MAX_STOCHASTICS_K, Double.class, DEFAULT_MAX_STOCHASTICS_K));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_BAR_COUNT, barCount);
        values.put(SETTING_MAX_STOCHASTICS_K, maxStochasticsK);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        switch (name) {
            case SETTING_BAR_COUNT -> barCount = (int) value;
            case SETTING_MAX_STOCHASTICS_K -> maxStochasticsK = (double) value;
            default -> { }
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        int required = barCount * 2;
        if (size < required) return false;
        int last = size - 1;
        float stdDevStochK = computeStdDevStochK(bars, last);
        if (Float.isNaN(stdDevStochK) || stdDevStochK >= maxStochasticsK) return false;
        float atrStochK = computeAtrStochK(bars, last);
        return !Float.isNaN(atrStochK) && atrStochK < maxStochasticsK;
    }

    private float computeStdDevStochK(Bars bars, int last) {
        float[] stdDevValues = computeStdDevSeries(bars, last);
        if (stdDevValues == null) return Float.NaN;
        return stochasticsK(stdDevValues);
    }

    private float computeAtrStochK(Bars bars, int last) {
        float[] atrValues = computeAtrSeries(bars, last);
        if (atrValues == null) return Float.NaN;
        return stochasticsK(atrValues);
    }

    private float[] computeStdDevSeries(Bars bars, int last) {
        int seriesLen = barCount;
        int startBar = last - seriesLen - barCount + 2;
        if (startBar < 0) return null;
        float[] series = new float[seriesLen];
        for (int s = 0; s < seriesLen; s++) {
            int endBar = startBar + barCount - 1 + s;
            float sum = 0f;
            int from = endBar - barCount + 1;
            for (int i = from; i <= endBar; i++) {
                sum += bars.getClose(i);
            }
            float mean = sum / barCount;
            float variance = 0f;
            for (int i = from; i <= endBar; i++) {
                float diff = bars.getClose(i) - mean;
                variance += diff * diff;
            }
            series[s] = (float) Math.sqrt(variance / barCount);
        }
        return series;
    }

    private float[] computeAtrSeries(Bars bars, int last) {
        int seriesLen = barCount;
        int startBar = last - seriesLen - barCount + 2;
        if (startBar < 1) return null;
        float[] series = new float[seriesLen];
        for (int s = 0; s < seriesLen; s++) {
            int endBar = startBar + barCount - 1 + s;
            float sum = 0f;
            int from = endBar - barCount + 1;
            for (int i = from; i <= endBar; i++) {
                float tr = bars.getHigh(i) - bars.getLow(i);
                if (i > 0) {
                    float prevClose = bars.getClose(i - 1);
                    tr = Math.max(tr, Math.max(
                            Math.abs(bars.getHigh(i) - prevClose),
                            Math.abs(bars.getLow(i) - prevClose)));
                }
                sum += tr;
            }
            series[s] = sum / barCount;
        }
        return series;
    }

    private float stochasticsK(float[] values) {
        float current = values[values.length - 1];
        float highest = values[0];
        float lowest = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > highest) highest = values[i];
            if (values[i] < lowest) lowest = values[i];
        }
        float range = highest - lowest;
        if (range <= 0f) return 0f;
        return (current - lowest) / range * 100f;
    }

}
