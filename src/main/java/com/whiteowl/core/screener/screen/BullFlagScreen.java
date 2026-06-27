package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BullFlagScreen implements Screen {

    private static final String NAME = "Bull Flag";
    private static final String SETTING_RANGE_BAR_COUNT = "Range Bar Count";
    private static final String SETTING_PATTERN_BAR_COUNT = "Pattern Bar Count";
    private static final String SETTING_MIN_FLAG_TO_POLE_RATIO = "Min Flag/Pole Ratio";
    private static final int DEFAULT_RANGE_BAR_COUNT = 144;
    private static final int DEFAULT_PATTERN_BAR_COUNT = 34;
    private static final double DEFAULT_MIN_FLAG_TO_POLE_RATIO = 2.0;
    private static final int SMA_PERIOD = 3;
    private static final float ONE_THIRD = 1f / 3f;

    private int rangeBarCount = DEFAULT_RANGE_BAR_COUNT;
    private int patternBarCount = DEFAULT_PATTERN_BAR_COUNT;
    private double minFlagToPoleDurationRatio = DEFAULT_MIN_FLAG_TO_POLE_RATIO;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_RANGE_BAR_COUNT, Integer.class, DEFAULT_RANGE_BAR_COUNT),
                new ScreenSetting(SETTING_PATTERN_BAR_COUNT, Integer.class, DEFAULT_PATTERN_BAR_COUNT),
                new ScreenSetting(SETTING_MIN_FLAG_TO_POLE_RATIO, Double.class, DEFAULT_MIN_FLAG_TO_POLE_RATIO));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_RANGE_BAR_COUNT, rangeBarCount);
        values.put(SETTING_PATTERN_BAR_COUNT, patternBarCount);
        values.put(SETTING_MIN_FLAG_TO_POLE_RATIO, minFlagToPoleDurationRatio);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        switch (name) {
            case SETTING_RANGE_BAR_COUNT -> rangeBarCount = (int) value;
            case SETTING_PATTERN_BAR_COUNT -> patternBarCount = (int) value;
            case SETTING_MIN_FLAG_TO_POLE_RATIO -> minFlagToPoleDurationRatio = (double) value;
            default -> { }
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        int required = rangeBarCount + SMA_PERIOD - 1;
        if (size < required) return false;
        float[] values = buildSmoothedTypicalPrice(bars, size);
        int last = size - 1;
        int patternStart = size - patternBarCount;
        int rangeStart = size - rangeBarCount;
        float current = values[last];
        int poleMaxIndex = findMaxIndex(values, patternStart, last);
        int poleMinIndex = findMinIndex(values, patternStart, last);
        float poleMax = values[poleMaxIndex];
        float poleMin = values[poleMinIndex];
        if (current >= poleMax || current <= poleMin) return false;
        int poleDuration = poleMaxIndex - poleMinIndex;
        if (poleDuration <= 0) return false;
        int flagDuration = last - poleMaxIndex;
        if (flagDuration < poleDuration * minFlagToPoleDurationRatio) return false;
        return isHighestInRange(values, poleMaxIndex, rangeStart);
    }

    private boolean isHighestInRange(float[] values, int poleMaxIndex, int rangeStart) {
        float poleMax = values[poleMaxIndex];
        for (int i = rangeStart; i < poleMaxIndex; i++) {
            if (values[i] >= poleMax) return false;
        }
        return true;
    }

    private float[] buildSmoothedTypicalPrice(Bars bars, int size) {
        float[] tp = new float[size];
        for (int i = 0; i < size; i++) {
            tp[i] = (bars.getHigh(i) + bars.getLow(i) + bars.getClose(i)) * ONE_THIRD;
        }
        float[] smoothed = new float[size];
        for (int i = 0; i < SMA_PERIOD - 1; i++) {
            smoothed[i] = tp[i];
        }
        for (int i = SMA_PERIOD - 1; i < size; i++) {
            float sum = 0f;
            for (int j = i - SMA_PERIOD + 1; j <= i; j++) {
                sum += tp[j];
            }
            smoothed[i] = sum / SMA_PERIOD;
        }
        return smoothed;
    }

    private int findMaxIndex(float[] values, int from, int to) {
        int bestIndex = from;
        for (int i = from + 1; i <= to; i++) {
            if (values[i] > values[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int findMinIndex(float[] values, int from, int to) {
        int bestIndex = from;
        for (int i = from + 1; i <= to; i++) {
            if (values[i] < values[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

}
