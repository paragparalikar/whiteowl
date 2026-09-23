package com.whiteowl.scripting.ranker.patternmatch;

import static com.whiteowl.scripting.ranker.patternmatch.SeriesTransform.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.scripting.examplegroup.model.Example;
import com.whiteowl.scripting.examplegroup.model.ExampleGroup;
import com.whiteowl.scripting.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.scripting.ranker.Ranker;
import com.whiteowl.scripting.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class PatternMatchRanker implements Ranker {

    private static final String RANKER_NAME = "Pattern Match";
    private static final String BAR_COUNT_SETTING = "Bar Count";
    private static final String EXAMPLE_GROUP_SETTING = "Example Group";
    private static final int DEFAULT_BAR_COUNT = 34;

    private final BarsRepository barsRepository;
    private final ExampleGroupRepository exampleGroupRepository;
    private final PercentileScorer percentileScorer = new PercentileScorer();
    private int barCount = DEFAULT_BAR_COUNT;
    private ExampleGroup selectedGroup;
    private boolean baselineDirty = true;
    private List<float[]> cachedNormalizedPatterns;

    public PatternMatchRanker(BarsRepository barsRepository, ExampleGroupRepository exampleGroupRepository) {
        this.barsRepository = barsRepository;
        this.exampleGroupRepository = exampleGroupRepository;
    }

    @Override
    public String getName() {
        return RANKER_NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        List<RankerSetting> settings = new ArrayList<>();
        settings.add(new RankerSetting(BAR_COUNT_SETTING, Integer.class, DEFAULT_BAR_COUNT));
        settings.add(new RankerSetting(EXAMPLE_GROUP_SETTING, ExampleGroup.class, null));
        return settings;
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(BAR_COUNT_SETTING, barCount);
        values.put(EXAMPLE_GROUP_SETTING, selectedGroup);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        switch (name) {
            case BAR_COUNT_SETTING -> {
                if (value instanceof Number n) barCount = n.intValue();
                baselineDirty = true;
            }
            case EXAMPLE_GROUP_SETTING -> {
                if (value instanceof ExampleGroup g) selectedGroup = g;
                baselineDirty = true;
            }
        }
    }

    public ExampleGroupRepository getExampleGroupRepository() {
        return exampleGroupRepository;
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        if (selectedGroup == null || selectedGroup.getExamples().isEmpty()) return null;
        if (bars.size() < barCount) return null;
        rebuildBaselineIfDirty();
        float[] candidateSeries = prepareCandidateSeries(bars);
        if (candidateSeries == null || candidateSeries.length == 0) return null;
        double bestDistance = Double.MAX_VALUE;
        for (float[] normalizedPattern : cachedNormalizedPatterns) {
            double distance = computeDistance(candidateSeries, normalizedPattern);
            bestDistance = Math.min(bestDistance, distance);
        }
        if (bestDistance == Double.MAX_VALUE) return null;
        return percentileScorer.toPercentile(bestDistance);
    }

    private float[] prepareCandidateSeries(Bars bars) {
        float[] allClose = extractClose(bars, bars.size());
        float[] tail = extractTail(allClose, barCount);
        return transformSeries(tail);
    }

    private float[] transformSeries(float[] closePrices) {
        return zScoreNormalize(toLogReturns(closePrices));
    }

    private void rebuildBaselineIfDirty() {
        if (!baselineDirty) return;
        baselineDirty = false;
        cachedNormalizedPatterns = new ArrayList<>();
        if (selectedGroup == null) return;
        for (Example example : selectedGroup.getExamples()) {
            float[] patternClose = loadPatternClose(example);
            if (patternClose == null || patternClose.length == 0) continue;
            float[] normalized = transformSeries(patternClose);
            if (normalized.length > 0) cachedNormalizedPatterns.add(normalized);
        }
        float[][] patternsArray = cachedNormalizedPatterns.toArray(new float[0][]);
        percentileScorer.buildBaseline(patternsArray);
    }

    private double computeDistance(float[] candidate, float[] pattern) {
        return Dtw.compute(candidate, pattern);
    }

    private float[] loadPatternClose(Example example) {
        try {
            Bars patternBars = barsRepository.load(example.getScripId(), example.getTimeframe());
            if (patternBars == null || patternBars.size() == 0) return null;
            int startIdx = findTimestampIndex(patternBars, example.getStartTimestamp());
            int endIdx = findTimestampIndex(patternBars, example.getEndTimestamp());
            if (startIdx < 0 || endIdx < 0 || startIdx > endIdx) return null;
            int length = endIdx - startIdx + 1;
            float[] close = new float[length];
            for (int i = 0; i < length; i++) {
                close[i] = patternBars.getClose(startIdx + i);
            }
            return close;
        } catch (Exception e) {
            log.debug("Failed to load pattern bars for {}: {}", example.getScripId(), e.getMessage());
            return null;
        }
    }

    private static int findTimestampIndex(Bars bars, long targetTimestamp) {
        int bestIdx = -1;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < bars.size(); i++) {
            long diff = Math.abs(bars.getTimestamp(i) - targetTimestamp);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

}
