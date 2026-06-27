package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TraditionalBreakoutScreen implements Screen {

    private static final String NAME = "Traditional Breakout";
    private static final String SETTING_LOOKBACK = "Lookback Bars";
    private static final String SETTING_CLOSE_PERCENTILE = "Close Percentile";
    private static final String SETTING_CLOSE_POSITION_PCT = "Close Position %";
    private static final String SETTING_SPREAD_PERCENTILE = "Spread Percentile";
    private static final String SETTING_VOLUME_SMA_PERIOD = "Volume SMA Period";
    private static final int DEFAULT_LOOKBACK = 50;
    private static final int DEFAULT_CLOSE_PERCENTILE = 98;
    private static final int DEFAULT_CLOSE_POSITION_PCT = 35;
    private static final int DEFAULT_SPREAD_PERCENTILE = 70;
    private static final int DEFAULT_VOLUME_SMA_PERIOD = 20;

    private int lookback = DEFAULT_LOOKBACK;
    private int closePercentile = DEFAULT_CLOSE_PERCENTILE;
    private int closePositionPct = DEFAULT_CLOSE_POSITION_PCT;
    private int spreadPercentile = DEFAULT_SPREAD_PERCENTILE;
    private int volumeSmaPeriod = DEFAULT_VOLUME_SMA_PERIOD;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_LOOKBACK, Integer.class, DEFAULT_LOOKBACK),
                new ScreenSetting(SETTING_CLOSE_PERCENTILE, Integer.class, DEFAULT_CLOSE_PERCENTILE),
                new ScreenSetting(SETTING_CLOSE_POSITION_PCT, Integer.class, DEFAULT_CLOSE_POSITION_PCT),
                new ScreenSetting(SETTING_SPREAD_PERCENTILE, Integer.class, DEFAULT_SPREAD_PERCENTILE),
                new ScreenSetting(SETTING_VOLUME_SMA_PERIOD, Integer.class, DEFAULT_VOLUME_SMA_PERIOD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_LOOKBACK, lookback);
        values.put(SETTING_CLOSE_PERCENTILE, closePercentile);
        values.put(SETTING_CLOSE_POSITION_PCT, closePositionPct);
        values.put(SETTING_SPREAD_PERCENTILE, spreadPercentile);
        values.put(SETTING_VOLUME_SMA_PERIOD, volumeSmaPeriod);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_LOOKBACK.equals(name)) {
            lookback = (int) value;
        } else if (SETTING_CLOSE_PERCENTILE.equals(name)) {
            closePercentile = (int) value;
        } else if (SETTING_CLOSE_POSITION_PCT.equals(name)) {
            closePositionPct = (int) value;
        } else if (SETTING_SPREAD_PERCENTILE.equals(name)) {
            spreadPercentile = (int) value;
        } else if (SETTING_VOLUME_SMA_PERIOD.equals(name)) {
            volumeSmaPeriod = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        int minBars = Math.max(lookback, volumeSmaPeriod) + 1;
        if (size < minBars) return false;
        int currentIndex = size - 1;
        float currentClose = bars.getClose(currentIndex);
        float currentHigh = bars.getHigh(currentIndex);
        float currentLow = bars.getLow(currentIndex);
        float currentSpread = currentHigh - currentLow;
        if (currentSpread <= 0) return false;
        if (!isCloseAboveHighPercentile(bars, currentIndex, currentClose)) return false;
        if (!isClosingInTopOfSpread(currentClose, currentHigh, currentLow)) return false;
        if (!isSpreadAbovePercentile(bars, currentIndex, currentSpread)) return false;
        return isVolumeAboveSma(bars, currentIndex);
    }

    private boolean isCloseAboveHighPercentile(Bars bars, int currentIndex, float currentClose) {
        float[] highs = new float[lookback];
        int from = currentIndex - lookback;
        for (int i = 0; i < lookback; i++) {
            highs[i] = bars.getHigh(from + i);
        }
        float threshold = percentile(highs, closePercentile);
        return currentClose >= threshold;
    }

    private boolean isClosingInTopOfSpread(float close, float high, float low) {
        float spread = high - low;
        float closePosition = (close - low) / spread;
        float topThreshold = 1.0f - (closePositionPct / 100.0f);
        return closePosition >= topThreshold;
    }

    private boolean isSpreadAbovePercentile(Bars bars, int currentIndex, float currentSpread) {
        float[] spreads = new float[lookback];
        int from = currentIndex - lookback;
        for (int i = 0; i < lookback; i++) {
            spreads[i] = bars.getHigh(from + i) - bars.getLow(from + i);
        }
        float threshold = percentile(spreads, spreadPercentile);
        return currentSpread > threshold;
    }

    private boolean isVolumeAboveSma(Bars bars, int currentIndex) {
        long currentVolume = bars.getVolume(currentIndex);
        long sum = 0;
        int from = currentIndex - volumeSmaPeriod;
        for (int i = 0; i < volumeSmaPeriod; i++) {
            sum += bars.getVolume(from + i);
        }
        double avgVolume = (double) sum / volumeSmaPeriod;
        return currentVolume > avgVolume;
    }

    private static float percentile(float[] values, int pct) {
        float[] sorted = values.clone();
        Arrays.sort(sorted);
        double rank = (pct / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = Math.min(lower + 1, sorted.length - 1);
        float fraction = (float) (rank - lower);
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower]);
    }

}
