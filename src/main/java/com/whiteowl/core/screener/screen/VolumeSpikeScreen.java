package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VolumeSpikeScreen implements Screen {

    private static final String NAME = "Volume Spike";
    private static final String SETTING_LOOKBACK = "Lookback Period";
    private static final String SETTING_MULTIPLIER = "Spike Multiplier";
    private static final int DEFAULT_LOOKBACK = 20;
    private static final double DEFAULT_MULTIPLIER = 2.0;

    private int lookback = DEFAULT_LOOKBACK;
    private double multiplier = DEFAULT_MULTIPLIER;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_LOOKBACK, Integer.class, DEFAULT_LOOKBACK),
                new ScreenSetting(SETTING_MULTIPLIER, Double.class, DEFAULT_MULTIPLIER));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_LOOKBACK, lookback);
        values.put(SETTING_MULTIPLIER, multiplier);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_LOOKBACK.equals(name)) {
            lookback = (int) value;
        } else if (SETTING_MULTIPLIER.equals(name)) {
            multiplier = ((Number) value).doubleValue();
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < lookback + 1) return false;
        long volumeSum = 0;
        for (int i = size - 1 - lookback; i < size - 1; i++) {
            volumeSum += bars.getVolume(i);
        }
        double avgVolume = (double) volumeSum / lookback;
        if (avgVolume == 0) return false;
        return bars.getVolume(size - 1) > avgVolume * multiplier;
    }

}
