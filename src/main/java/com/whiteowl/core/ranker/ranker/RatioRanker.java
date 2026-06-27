package com.whiteowl.core.ranker.ranker;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class RatioRanker implements Ranker {

    private static final String NAME = "Ratio";
    private static final String SETTING_COMPARISON_SCRIP = "Comparison Scrip";
    private static final String SETTING_PERIOD = "Period";
    private static final String DEFAULT_COMPARISON_SCRIP = "NSE:NIFTY TOTAL MKT";
    private static final int DEFAULT_PERIOD = 50;

    private final BarsRepository barsRepository;
    private String comparisonScripId = DEFAULT_COMPARISON_SCRIP;
    private int period = DEFAULT_PERIOD;

    public RatioRanker(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        return List.of(
                new RankerSetting(SETTING_COMPARISON_SCRIP, Scrip.class, DEFAULT_COMPARISON_SCRIP),
                new RankerSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD)
        );
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_COMPARISON_SCRIP, comparisonScripId);
        values.put(SETTING_PERIOD, period);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_COMPARISON_SCRIP.equals(name) && value instanceof String s) {
            comparisonScripId = s;
        } else if (SETTING_PERIOD.equals(name) && value instanceof Integer i) {
            period = i;
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size <= period) return null;
        Bars compBars = loadComparisonBars(bars.getTimeframe());
        if (compBars == null || compBars.size() == 0) return null;
        int startIdx = size - period - 1;
        int endIdx = size - 1;
        float scripStart = bars.getClose(startIdx);
        float scripEnd = bars.getClose(endIdx);
        if (scripStart <= 0) return null;
        long startTs = bars.getTimestamp(startIdx);
        long endTs = bars.getTimestamp(endIdx);
        int compStartIdx = findTimestampIndex(compBars, startTs);
        int compEndIdx = findTimestampIndex(compBars, endTs);
        if (compStartIdx >= compBars.size() || compBars.getTimestamp(compStartIdx) != startTs) return null;
        if (compEndIdx >= compBars.size() || compBars.getTimestamp(compEndIdx) != endTs) return null;
        float compStart = compBars.getClose(compStartIdx);
        float compEnd = compBars.getClose(compEndIdx);
        if (compStart <= 0) return null;
        double relativeStrength = (scripEnd / scripStart) / (compEnd / compStart);
        return Math.log(relativeStrength);
    }

    private Bars loadComparisonBars(Timeframe timeframe) {
        try {
            Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
            int totalBars = barsRepository.countBars(comparisonScripId, loadTf);
            if (totalBars == 0) return null;
            return barsRepository.loadRange(comparisonScripId, loadTf, 0, totalBars);
        } catch (IOException e) {
            log.error("Failed to load comparison bars for {}", comparisonScripId, e);
            return null;
        }
    }

    private int findTimestampIndex(Bars bars, long targetTs) {
        for (int i = 0; i < bars.size(); i++) {
            if (bars.getTimestamp(i) >= targetTs) return i;
        }
        return bars.size();
    }

}
