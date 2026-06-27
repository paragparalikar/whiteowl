package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.trendline.Trendline;
import com.whiteowl.core.trendline.TrendlineDetector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.trendline.TrendlineDirection.DESCENDING;

public final class ResistanceBreakoutScreen implements Screen {

    private static final String NAME = "Resistance Breakout";
    private static final String SETTING_PIVOT_LOOKBACK = "Pivot Lookback";
    private static final String SETTING_TOUCH_TOLERANCE = "Touch Tolerance %";
    private static final String SETTING_MIN_TOUCHES = "Min Touches";
    private static final String SETTING_MAX_VIOLATIONS = "Max Violations";
    private static final String SETTING_LOOKBACK_BARS = "Lookback Bars";
    private static final int DEFAULT_PIVOT_LOOKBACK = 5;
    private static final double DEFAULT_TOUCH_TOLERANCE = 0.3;
    private static final int DEFAULT_MIN_TOUCHES = 3;
    private static final int DEFAULT_MAX_VIOLATIONS = 2;
    private static final int DEFAULT_LOOKBACK_BARS = 200;
    private static final double TOLERANCE_DIVISOR = 100.0;
    private static final int MIN_BARS_REQUIRED = 30;

    private int pivotLookback = DEFAULT_PIVOT_LOOKBACK;
    private double touchTolerancePct = DEFAULT_TOUCH_TOLERANCE;
    private int minTouches = DEFAULT_MIN_TOUCHES;
    private int maxViolations = DEFAULT_MAX_VIOLATIONS;
    private int lookbackBars = DEFAULT_LOOKBACK_BARS;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_PIVOT_LOOKBACK, Integer.class, DEFAULT_PIVOT_LOOKBACK),
                new ScreenSetting(SETTING_TOUCH_TOLERANCE, Double.class, DEFAULT_TOUCH_TOLERANCE),
                new ScreenSetting(SETTING_MIN_TOUCHES, Integer.class, DEFAULT_MIN_TOUCHES),
                new ScreenSetting(SETTING_MAX_VIOLATIONS, Integer.class, DEFAULT_MAX_VIOLATIONS),
                new ScreenSetting(SETTING_LOOKBACK_BARS, Integer.class, DEFAULT_LOOKBACK_BARS));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PIVOT_LOOKBACK, pivotLookback);
        values.put(SETTING_TOUCH_TOLERANCE, touchTolerancePct);
        values.put(SETTING_MIN_TOUCHES, minTouches);
        values.put(SETTING_MAX_VIOLATIONS, maxViolations);
        values.put(SETTING_LOOKBACK_BARS, lookbackBars);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        switch (name) {
            case SETTING_PIVOT_LOOKBACK -> pivotLookback = ((Number) value).intValue();
            case SETTING_TOUCH_TOLERANCE -> touchTolerancePct = ((Number) value).doubleValue();
            case SETTING_MIN_TOUCHES -> minTouches = ((Number) value).intValue();
            case SETTING_MAX_VIOLATIONS -> maxViolations = ((Number) value).intValue();
            case SETTING_LOOKBACK_BARS -> lookbackBars = ((Number) value).intValue();
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < MIN_BARS_REQUIRED) return false;
        Bars window = trimToLookback(bars, size);
        List<Trendline> lines = buildDetector().findResistanceLines(window, DESCENDING);
        if (lines.isEmpty()) return false;
        return hasBreakout(window, lines);
    }

    private Bars trimToLookback(Bars bars, int size) {
        if (size <= lookbackBars) return bars;
        int start = size - lookbackBars;
        Bars window = new Bars(bars.getScripId(), bars.getTimeframe(), lookbackBars);
        for (int i = start; i < size; i++) {
            window.append(bars.getTimestamp(i), bars.getOpen(i), bars.getHigh(i),
                    bars.getLow(i), bars.getClose(i), bars.getVolume(i));
        }
        return window;
    }

    private TrendlineDetector buildDetector() {
        return new TrendlineDetector()
                .pivotLookback(pivotLookback)
                .useCloseForPivots(true)
                .touchTolerance(touchTolerancePct / TOLERANCE_DIVISOR)
                .minTouches(minTouches)
                .maxViolations(maxViolations)
                .maxResults(1);
    }

    private boolean hasBreakout(Bars bars, List<Trendline> lines) {
        int lastIndex = bars.size() - 1;
        if (lastIndex < 1) return false;
        for (Trendline line : lines) {
            double resistanceNow = line.priceAt(lastIndex);
            double resistancePrev = line.priceAt(lastIndex - 1);
            float closeNow = bars.getClose(lastIndex);
            float closePrev = bars.getClose(lastIndex - 1);
            boolean wasBelowOrAt = closePrev <= resistancePrev;
            boolean isAbove = closeNow > resistanceNow;
            if (wasBelowOrAt && isAbove) return true;
        }
        return false;
    }

}
