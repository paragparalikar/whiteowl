package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LiquidityScreen implements Screen {

    private static final String NAME = "Liquidity";
    private static final String SETTING_MIN_TURNOVER = "Min Turnover";
    private static final String SETTING_MAX_TURNOVER = "Max Turnover";
    private static final String SETTING_MIN_SPREAD_PCT = "Min Spread %";
    private static final String SETTING_BAR_COUNT = "Bar Count";
    private static final double DEFAULT_MIN_TURNOVER = 10_000_000;
    private static final double DEFAULT_MAX_TURNOVER = 500_000_000;
    private static final double DEFAULT_MIN_SPREAD_PCT = 0.1;
    private static final int DEFAULT_BAR_COUNT = 50;
    private static final double PERCENTAGE_FACTOR = 100.0;

    private double minTurnover = DEFAULT_MIN_TURNOVER;
    private double maxTurnover = DEFAULT_MAX_TURNOVER;
    private double minSpreadPercentage = DEFAULT_MIN_SPREAD_PCT;
    private int barCount = DEFAULT_BAR_COUNT;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_MIN_TURNOVER, Double.class, DEFAULT_MIN_TURNOVER),
                new ScreenSetting(SETTING_MAX_TURNOVER, Double.class, DEFAULT_MAX_TURNOVER),
                new ScreenSetting(SETTING_MIN_SPREAD_PCT, Double.class, DEFAULT_MIN_SPREAD_PCT),
                new ScreenSetting(SETTING_BAR_COUNT, Integer.class, DEFAULT_BAR_COUNT));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_MIN_TURNOVER, minTurnover);
        values.put(SETTING_MAX_TURNOVER, maxTurnover);
        values.put(SETTING_MIN_SPREAD_PCT, minSpreadPercentage);
        values.put(SETTING_BAR_COUNT, barCount);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_MIN_TURNOVER.equals(name)) {
            minTurnover = ((Number) value).doubleValue();
        } else if (SETTING_MAX_TURNOVER.equals(name)) {
            maxTurnover = ((Number) value).doubleValue();
        } else if (SETTING_MIN_SPREAD_PCT.equals(name)) {
            minSpreadPercentage = ((Number) value).doubleValue();
        } else if (SETTING_BAR_COUNT.equals(name)) {
            barCount = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < barCount) return false;
        int start = size - barCount;
        for (int i = start; i < size; i++) {
            if (!isBarLiquid(bars, i)) return false;
        }
        return true;
    }

    private boolean isBarLiquid(Bars bars, int i) {
        float high = bars.getHigh(i);
        float low = bars.getLow(i);
        float close = bars.getClose(i);
        float typicalPrice = (high + low + close) / 3f;
        double turnover = typicalPrice * bars.getVolume(i);
        if (turnover < minTurnover || turnover > maxTurnover) return false;
        if (high == 0) return false;
        double spread = high - low;
        double spreadPct = spread * PERCENTAGE_FACTOR / high;
        return spreadPct >= minSpreadPercentage;
    }

}
