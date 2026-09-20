package com.whiteowl.scripting.ranker.builtin;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.scripting.ranker.Ranker;
import com.whiteowl.scripting.ranker.RankerSetting;

import java.util.List;
import java.util.Map;

public final class GainersRanker implements Ranker {

    private static final String RANKER_NAME = "Gainers";
    static final String PERIOD_SETTING = "Period";
    static final int DEFAULT_PERIOD = 5;

    private int period = DEFAULT_PERIOD;

    @Override
    public String getName() {
        return RANKER_NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        return List.of(new RankerSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        return Map.of(PERIOD_SETTING, period);
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (PERIOD_SETTING.equals(name) && value instanceof Number n) {
            period = Math.max(1, n.intValue());
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        Double pct = PercentChange.compute(bars, period);
        if (pct == null || pct <= 0) return null;
        return pct;
    }

}
