package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PivotBreakoutScreen implements Screen {

    private static final String NAME = "Pivot Breakout";
    private static final String SETTING_PIVOT_STRENGTH = "Pivot Strength";
    private static final String SETTING_LOOKBACK = "Lookback Bars";
    private static final int DEFAULT_PIVOT_STRENGTH = 5;
    private static final int DEFAULT_LOOKBACK = 21;

    private int pivotStrength = DEFAULT_PIVOT_STRENGTH;
    private int lookback = DEFAULT_LOOKBACK;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_PIVOT_STRENGTH, Integer.class, DEFAULT_PIVOT_STRENGTH),
                new ScreenSetting(SETTING_LOOKBACK, Integer.class, DEFAULT_LOOKBACK));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PIVOT_STRENGTH, pivotStrength);
        values.put(SETTING_LOOKBACK, lookback);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PIVOT_STRENGTH.equals(name)) {
            pivotStrength = (int) value;
        } else if (SETTING_LOOKBACK.equals(name)) {
            lookback = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        int minBars = lookback + pivotStrength + 1;
        if (size < minBars) return false;
        int lastBarIndex = size - 1;
        float latestClose = bars.getClose(lastBarIndex);
        int searchStart = lastBarIndex - lookback;
        int searchEnd = lastBarIndex - pivotStrength;
        float pivotHigh = Float.NaN;
        for (int i = searchEnd; i >= searchStart; i--) {
            if (isPivotHigh(bars, i)) {
                pivotHigh = bars.getHigh(i);
                break;
            }
        }
        if (Float.isNaN(pivotHigh)) return false;
        return latestClose >= pivotHigh;
    }

    private boolean isPivotHigh(Bars bars, int index) {
        float candidateHigh = bars.getHigh(index);
        for (int j = 1; j <= pivotStrength; j++) {
            if (bars.getHigh(index - j) >= candidateHigh) return false;
            if (bars.getHigh(index + j) >= candidateHigh) return false;
        }
        return true;
    }

}
