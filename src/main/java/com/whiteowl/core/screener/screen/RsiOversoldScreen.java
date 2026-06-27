package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RsiOversoldScreen implements Screen {

    private static final String NAME = "RSI Oversold";
    private static final String SETTING_PERIOD = "RSI Period";
    private static final String SETTING_THRESHOLD = "Threshold";
    private static final int DEFAULT_PERIOD = 14;
    private static final double DEFAULT_THRESHOLD = 30.0;

    private int period = DEFAULT_PERIOD;
    private double threshold = DEFAULT_THRESHOLD;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD),
                new ScreenSetting(SETTING_THRESHOLD, Double.class, DEFAULT_THRESHOLD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PERIOD, period);
        values.put(SETTING_THRESHOLD, threshold);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PERIOD.equals(name)) {
            period = (int) value;
        } else if (SETTING_THRESHOLD.equals(name)) {
            threshold = ((Number) value).doubleValue();
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < period + 1) return false;
        double gainSum = 0;
        double lossSum = 0;
        for (int i = size - period; i < size; i++) {
            double change = bars.getClose(i) - bars.getClose(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum -= change;
            }
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        if (avgLoss == 0) return false;
        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        return rsi < threshold;
    }

}
