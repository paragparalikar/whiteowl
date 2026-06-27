package com.whiteowl.core.ranker.ranker;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AccumulationRanker implements Ranker {

    private static final String NAME = "Accumulation";
    private static final String SETTING_BAR_COUNT = "Bar Count";
    private static final int DEFAULT_BAR_COUNT = 14;

    private int barCount = DEFAULT_BAR_COUNT;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        return List.of(new RankerSetting(SETTING_BAR_COUNT, Integer.class, DEFAULT_BAR_COUNT));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_BAR_COUNT, barCount);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_BAR_COUNT.equals(name)) {
            barCount = (int) value;
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        int size = bars.size();
        int required = barCount * 2 + 1;
        if (size < required) return null;
        int endBar = size - 1;
        return (double) computeAdx(bars, endBar);
    }

    private float computeAdx(Bars bars, int endBar) {
        int len = barCount * 2;
        float[] plusDm = new float[len];
        float[] minusDm = new float[len];
        float[] tr = new float[len];
        int start = endBar - len + 1;
        for (int i = 0; i < len; i++) {
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
        float smoothTr = sum(tr, 0, barCount);
        float smoothPlusDm = sum(plusDm, 0, barCount);
        float smoothMinusDm = sum(minusDm, 0, barCount);
        float[] dx = new float[barCount];
        for (int i = 0; i < barCount; i++) {
            int j = barCount + i;
            smoothTr = smoothTr - smoothTr / barCount + tr[j];
            smoothPlusDm = smoothPlusDm - smoothPlusDm / barCount + plusDm[j];
            smoothMinusDm = smoothMinusDm - smoothMinusDm / barCount + minusDm[j];
            float plusDi = (smoothTr > 0) ? smoothPlusDm / smoothTr * 100 : 0;
            float minusDi = (smoothTr > 0) ? smoothMinusDm / smoothTr * 100 : 0;
            float diSum = plusDi + minusDi;
            dx[i] = (diSum > 0) ? Math.abs(plusDi - minusDi) / diSum * 100 : 0;
        }
        return sum(dx, 0, barCount) / barCount;
    }

    private float sum(float[] values, int from, int length) {
        float s = 0;
        for (int i = from; i < from + length; i++) {
            s += values[i];
        }
        return s;
    }

}
