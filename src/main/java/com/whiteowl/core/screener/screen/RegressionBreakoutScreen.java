package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegressionBreakoutScreen implements Screen {

    private static final String NAME = "Regression Breakout";
    private static final String SETTING_PERIOD = "Period";
    private static final String SETTING_ATR_MULTIPLIER = "ATR Multiplier";
    private static final int DEFAULT_PERIOD = 5;
    private static final double DEFAULT_ATR_MULTIPLIER = 0.33;

    private int period = DEFAULT_PERIOD;
    private double atrMultiplier = DEFAULT_ATR_MULTIPLIER;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD),
                new ScreenSetting(SETTING_ATR_MULTIPLIER, Double.class, DEFAULT_ATR_MULTIPLIER));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PERIOD, period);
        values.put(SETTING_ATR_MULTIPLIER, atrMultiplier);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PERIOD.equals(name)) {
            period = (int) value;
        } else if (SETTING_ATR_MULTIPLIER.equals(name)) {
            atrMultiplier = ((Number) value).doubleValue();
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < period + 2) return false;
        double predictedClose = computePredictedClose(bars, size);
        double atr = computeAtr(bars, size);
        if (atr == 0) return false;
        float actualClose = bars.getClose(size - 1);
        return actualClose >= predictedClose + (atrMultiplier * atr);
    }

    private double computePredictedClose(Bars bars, int size) {
        int start = size - 1 - period;
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        for (int i = 0; i < period; i++) {
            double x = i;
            double y = bars.getClose(start + i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double slope = (period * sumXY - sumX * sumY) / (period * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / period;
        return intercept + slope * period;
    }

    private double computeAtr(Bars bars, int size) {
        int start = size - period;
        double trSum = 0;
        for (int i = start; i < size; i++) {
            float high = bars.getHigh(i);
            float low = bars.getLow(i);
            float prevClose = bars.getClose(i - 1);
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            trSum += tr;
        }
        return trSum / period;
    }

}
