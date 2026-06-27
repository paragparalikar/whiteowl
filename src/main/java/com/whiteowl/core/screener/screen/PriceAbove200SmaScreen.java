package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PriceAbove200SmaScreen implements Screen {

    private static final String NAME = "Price > SMA";
    private static final String SETTING_PERIOD = "SMA Period";
    private static final int DEFAULT_PERIOD = 200;

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
        if (size < period) return false;
        double sum = 0;
        for (int i = size - period; i < size; i++) {
            sum += bars.getClose(i);
        }
        double sma = sum / period;
        return bars.getClose(size - 1) > sma;
    }

}
