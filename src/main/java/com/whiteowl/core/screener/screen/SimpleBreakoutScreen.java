package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleBreakoutScreen implements Screen {

    private static final String NAME = "Simple Breakout";
    private static final String SETTING_PERIOD = "Period";
    private static final int DEFAULT_PERIOD = 5;

    private int period = DEFAULT_PERIOD;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(new ScreenSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PERIOD, period);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PERIOD.equals(name)) {
            period = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < period + 1) return false;
        float currentClose = bars.getClose(size - 1);
        float highestHigh = Float.MIN_VALUE;
        for (int i = size - 1 - period; i < size - 1; i++) {
            highestHigh = Math.max(highestHigh, bars.getHigh(i));
        }
        return currentClose > highestHigh;
    }

}
