package com.whiteowl.core.ranker.ranker;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GainersRanker implements Ranker {

    private static final String NAME = "Gainers";
    private static final String SETTING_PERIOD = "Period";
    private static final int DEFAULT_PERIOD = 50;
    private static final double HUNDRED = 100.0;

    private int period = DEFAULT_PERIOD;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        return List.of(new RankerSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_PERIOD, period);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PERIOD.equals(name) && value instanceof Integer i) {
            period = i;
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < period) return null;
        int start = size - period;
        int last = size - 1;
        float lastClose = bars.getClose(last);
        float firstClose = bars.getClose(start);
        if (lastClose <= firstClose) return null;
        float lowestLow = bars.getLow(start);
        for (int i = start + 1; i < size; i++) {
            float low = bars.getLow(i);
            if (low < lowestLow) lowestLow = low;
        }
        if (lowestLow <= 0) return null;
        double gain = (lastClose - lowestLow) / lowestLow * HUNDRED;
        if (gain <= 0) return null;
        return gain;
    }

}
