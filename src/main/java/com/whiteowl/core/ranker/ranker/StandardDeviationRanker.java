package com.whiteowl.core.ranker.ranker;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StandardDeviationRanker implements Ranker {

    private static final String NAME = "Standard Deviation";
    private static final String SETTING_PERIOD = "Period";
    private static final int DEFAULT_PERIOD = 20;

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
        if (SETTING_PERIOD.equals(name)) {
            period = (int) value;
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < period) return null;
        double sum = 0;
        for (int i = size - period; i < size; i++) {
            sum += bars.getClose(i);
        }
        double mean = sum / period;
        double sqSum = 0;
        for (int i = size - period; i < size; i++) {
            double diff = bars.getClose(i) - mean;
            sqSum += diff * diff;
        }
        return Math.sqrt(sqSum / period);
    }

}
