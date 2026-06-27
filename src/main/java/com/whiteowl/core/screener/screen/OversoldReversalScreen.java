package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OversoldReversalScreen implements Screen {

    private static final String NAME = "Oversold Reversal";
    private static final String SETTING_RSI_PERIOD = "RSI Period";
    private static final String SETTING_RSI_THRESHOLD = "RSI Threshold";
    private static final String SETTING_CLOSE_POSITION_PCT = "Close Position %";
    private static final int DEFAULT_RSI_PERIOD = 13;
    private static final double DEFAULT_RSI_THRESHOLD = 30.0;
    private static final int DEFAULT_CLOSE_POSITION_PCT = 40;

    private int rsiPeriod = DEFAULT_RSI_PERIOD;
    private double rsiThreshold = DEFAULT_RSI_THRESHOLD;
    private int closePositionPct = DEFAULT_CLOSE_POSITION_PCT;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_RSI_PERIOD, Integer.class, DEFAULT_RSI_PERIOD),
                new ScreenSetting(SETTING_RSI_THRESHOLD, Double.class, DEFAULT_RSI_THRESHOLD),
                new ScreenSetting(SETTING_CLOSE_POSITION_PCT, Integer.class, DEFAULT_CLOSE_POSITION_PCT));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_RSI_PERIOD, rsiPeriod);
        values.put(SETTING_RSI_THRESHOLD, rsiThreshold);
        values.put(SETTING_CLOSE_POSITION_PCT, closePositionPct);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_RSI_PERIOD.equals(name)) {
            rsiPeriod = (int) value;
        } else if (SETTING_RSI_THRESHOLD.equals(name)) {
            rsiThreshold = ((Number) value).doubleValue();
        } else if (SETTING_CLOSE_POSITION_PCT.equals(name)) {
            closePositionPct = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < rsiPeriod + 2) return false;
        if (!isPreviousBarOversold(bars, size)) return false;
        if (!isCloseAbovePreviousHigh(bars, size)) return false;
        if (!isGreenBar(bars, size)) return false;
        return isClosingInTopOfSpread(bars, size);
    }

    private boolean isPreviousBarOversold(Bars bars, int size) {
        int prevBarIndex = size - 2;
        int from = prevBarIndex - rsiPeriod + 1;
        double gainSum = 0;
        double lossSum = 0;
        for (int i = from + 1; i <= prevBarIndex; i++) {
            double change = bars.getClose(i) - bars.getClose(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum -= change;
            }
        }
        double avgGain = gainSum / rsiPeriod;
        double avgLoss = lossSum / rsiPeriod;
        if (avgLoss == 0) return false;
        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        return rsi < rsiThreshold;
    }

    private boolean isCloseAbovePreviousHigh(Bars bars, int size) {
        float currentClose = bars.getClose(size - 1);
        float previousHigh = bars.getHigh(size - 2);
        return currentClose >= previousHigh;
    }

    private boolean isGreenBar(Bars bars, int size) {
        int currentIndex = size - 1;
        return bars.getClose(currentIndex) > bars.getOpen(currentIndex);
    }

    private boolean isClosingInTopOfSpread(Bars bars, int size) {
        int currentIndex = size - 1;
        float high = bars.getHigh(currentIndex);
        float low = bars.getLow(currentIndex);
        float spread = high - low;
        if (spread <= 0) return false;
        float closePosition = (bars.getClose(currentIndex) - low) / spread;
        float topThreshold = 1.0f - (closePositionPct / 100.0f);
        return closePosition >= topThreshold;
    }

}
