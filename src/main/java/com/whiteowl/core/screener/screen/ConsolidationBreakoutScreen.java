package com.whiteowl.core.screener.screen;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConsolidationBreakoutScreen implements Screen {

    private static final String NAME = "Consolidation Breakout";
    private static final String SETTING_CONSOLIDATION_PERIOD = "Consolidation Period";
    private static final String SETTING_BREAKOUT_PERIOD = "Breakout Period";
    private static final String SETTING_MAX_VOLATILITY = "Max Volatility";
    private static final String SETTING_MAX_ADX = "Max ADX";
    private static final int DEFAULT_CONSOLIDATION_PERIOD = 100;
    private static final int DEFAULT_BREAKOUT_PERIOD = 30;
    private static final double DEFAULT_MAX_VOLATILITY = 5.0;
    private static final double DEFAULT_MAX_ADX = 25.0;
    private static final int VOLATILITY_OFFSET = 2;

    private int consolidationPeriod = DEFAULT_CONSOLIDATION_PERIOD;
    private int breakoutPeriod = DEFAULT_BREAKOUT_PERIOD;
    private double maxVolatility = DEFAULT_MAX_VOLATILITY;
    private double maxAdx = DEFAULT_MAX_ADX;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_CONSOLIDATION_PERIOD, Integer.class, DEFAULT_CONSOLIDATION_PERIOD),
                new ScreenSetting(SETTING_BREAKOUT_PERIOD, Integer.class, DEFAULT_BREAKOUT_PERIOD),
                new ScreenSetting(SETTING_MAX_VOLATILITY, Double.class, DEFAULT_MAX_VOLATILITY),
                new ScreenSetting(SETTING_MAX_ADX, Double.class, DEFAULT_MAX_ADX));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_CONSOLIDATION_PERIOD, consolidationPeriod);
        values.put(SETTING_BREAKOUT_PERIOD, breakoutPeriod);
        values.put(SETTING_MAX_VOLATILITY, maxVolatility);
        values.put(SETTING_MAX_ADX, maxAdx);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        switch (name) {
            case SETTING_CONSOLIDATION_PERIOD -> consolidationPeriod = (int) value;
            case SETTING_BREAKOUT_PERIOD -> breakoutPeriod = (int) value;
            case SETTING_MAX_VOLATILITY -> maxVolatility = (double) value;
            case SETTING_MAX_ADX -> maxAdx = (double) value;
            default -> { }
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < consolidationPeriod + consolidationPeriod) return false;
        int last = size - 1;
        if (!isGreenCandle(bars, last)) return false;
        if (!isCloseAbovePreviousHigh(bars, last)) return false;
        if (!isHighestHigh(bars, last)) return false;
        float stochK = computeTypicalPriceStdDevStochK(bars, last - VOLATILITY_OFFSET);
        if (Float.isNaN(stochK) || stochK >= maxVolatility) return false;
        float adx = computeAdx(bars, last - VOLATILITY_OFFSET);
        return !Float.isNaN(adx) && adx < maxAdx;
    }

    private boolean isGreenCandle(Bars bars, int index) {
        return bars.getClose(index) > bars.getOpen(index);
    }

    private boolean isCloseAbovePreviousHigh(Bars bars, int index) {
        return index > 0 && bars.getClose(index) >= bars.getHigh(index - 1);
    }

    private boolean isHighestHigh(Bars bars, int index) {
        float currentHigh = bars.getHigh(index);
        int from = Math.max(0, index - breakoutPeriod + 1);
        for (int i = from; i < index; i++) {
            if (bars.getHigh(i) >= currentHigh) return false;
        }
        return true;
    }

    private float computeTypicalPriceStdDevStochK(Bars bars, int last) {
        float[] stdDevSeries = computeStdDevSeries(bars, last);
        if (stdDevSeries == null) return Float.NaN;
        return stochasticsK(stdDevSeries);
    }

    private float[] computeStdDevSeries(Bars bars, int last) {
        int seriesLen = consolidationPeriod;
        int startBar = last - seriesLen - consolidationPeriod + 2;
        if (startBar < 0) return null;
        float[] series = new float[seriesLen];
        for (int s = 0; s < seriesLen; s++) {
            int endBar = startBar + consolidationPeriod - 1 + s;
            series[s] = computeStdDev(bars, endBar);
        }
        return series;
    }

    private float computeStdDev(Bars bars, int endBar) {
        int from = endBar - consolidationPeriod + 1;
        float sum = 0f;
        for (int i = from; i <= endBar; i++) {
            sum += typicalPrice(bars, i);
        }
        float mean = sum / consolidationPeriod;
        float variance = 0f;
        for (int i = from; i <= endBar; i++) {
            float diff = typicalPrice(bars, i) - mean;
            variance += diff * diff;
        }
        return (float) Math.sqrt(variance / consolidationPeriod);
    }

    private float typicalPrice(Bars bars, int index) {
        return (bars.getHigh(index) + bars.getLow(index) + bars.getClose(index)) / 3f;
    }

    private float computeAdx(Bars bars, int endBar) {
        int required = breakoutPeriod * 2 + 1;
        if (endBar < required) return Float.NaN;
        float[] plusDm = new float[breakoutPeriod * 2];
        float[] minusDm = new float[breakoutPeriod * 2];
        float[] tr = new float[breakoutPeriod * 2];
        int start = endBar - breakoutPeriod * 2 + 1;
        for (int i = 0; i < breakoutPeriod * 2; i++) {
            int idx = start + i;
            float high = bars.getHigh(idx);
            float low = bars.getLow(idx);
            float prevHigh = bars.getHigh(idx - 1);
            float prevLow = bars.getLow(idx - 1);
            float prevClose = bars.getClose(idx - 1);
            float upMove = high - prevHigh;
            float downMove = prevLow - low;
            plusDm[i] = (upMove > downMove && upMove > 0) ? upMove : 0;
            minusDm[i] = (downMove > upMove && downMove > 0) ? downMove : 0;
            tr[i] = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
        }
        float smoothTr = sum(tr, 0, breakoutPeriod);
        float smoothPlusDm = sum(plusDm, 0, breakoutPeriod);
        float smoothMinusDm = sum(minusDm, 0, breakoutPeriod);
        float[] dx = new float[breakoutPeriod];
        for (int i = 0; i < breakoutPeriod; i++) {
            int j = breakoutPeriod + i;
            smoothTr = smoothTr - smoothTr / breakoutPeriod + tr[j];
            smoothPlusDm = smoothPlusDm - smoothPlusDm / breakoutPeriod + plusDm[j];
            smoothMinusDm = smoothMinusDm - smoothMinusDm / breakoutPeriod + minusDm[j];
            float plusDi = (smoothTr > 0) ? smoothPlusDm / smoothTr * 100 : 0;
            float minusDi = (smoothTr > 0) ? smoothMinusDm / smoothTr * 100 : 0;
            float diSum = plusDi + minusDi;
            dx[i] = (diSum > 0) ? Math.abs(plusDi - minusDi) / diSum * 100 : 0;
        }
        return sum(dx, 0, breakoutPeriod) / breakoutPeriod;
    }

    private float sum(float[] values, int from, int length) {
        float s = 0;
        for (int i = from; i < from + length; i++) {
            s += values[i];
        }
        return s;
    }

    private float stochasticsK(float[] values) {
        float current = values[values.length - 1];
        float highest = values[0];
        float lowest = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > highest) highest = values[i];
            if (values[i] < lowest) lowest = values[i];
        }
        float range = highest - lowest;
        if (range <= 0f) return 0f;
        return (current - lowest) / range * 100f;
    }

}
