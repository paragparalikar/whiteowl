package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Scrip;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public final class RatioIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Ratio";
    private static final String SETTING_COMPARISON_SCRIP = "Comparison Scrip";
    private static final String SETTING_PERIOD = "Period";
    private static final String DEFAULT_COMPARISON_SCRIP = "NSE:NIFTY TOTAL MKT";
    private static final int DEFAULT_PERIOD = 50;
    private static final Color SYNC_COLOR = Color.web("#4dabf7");
    private static final Color POSITIVE_DIVERGENCE_COLOR = Color.web("#51cf66");
    private static final Color NEGATIVE_DIVERGENCE_COLOR = Color.web("#ff6b6b");

    private final BarsRepository barsRepository;

    public RatioIndicator(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(SETTING_COMPARISON_SCRIP, Scrip.class, DEFAULT_COMPARISON_SCRIP),
                new IndicatorSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        String compScripId = resolveString(settings, SETTING_COMPARISON_SCRIP, DEFAULT_COMPARISON_SCRIP);
        int period = resolveInt(settings, SETTING_PERIOD, DEFAULT_PERIOD);
        int size = bars.size();
        double[] values = new double[size];
        initializeEmpty(values, size);
        Bars compBars = loadComparisonBars(compScripId, bars.getTimeframe());
        double[] posDiv = new double[size];
        double[] negDiv = new double[size];
        initializeEmpty(posDiv, size);
        initializeEmpty(negDiv, size);
        if (compBars == null || compBars.size() == 0) {
            return buildResult(values, posDiv, negDiv, compScripId, period);
        }
        computeRollingRatio(bars, compBars, values, posDiv, negDiv, period);
        return buildResult(values, posDiv, negDiv, compScripId, period);
    }

    private Bars loadComparisonBars(String scripId, Timeframe timeframe) {
        try {
            Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
            int count = barsRepository.countBars(scripId, loadTf);
            if (count == 0) return null;
            return barsRepository.loadRange(scripId, loadTf, 0, count);
        } catch (IOException e) {
            log.error("Failed to load comparison bars for {}", scripId, e);
            return null;
        }
    }

    private void computeRollingRatio(Bars bars, Bars compBars, double[] sync, double[] posDiv, double[] negDiv, int period) {
        int size = bars.size();
        int[] compIndexMap = buildCompIndexMap(bars, compBars);
        for (int i = period; i < size; i++) {
            int startBar = i - period;
            int compStartIdx = compIndexMap[startBar];
            int compEndIdx = compIndexMap[i];
            if (compStartIdx < 0 || compEndIdx < 0) continue;
            float scripStart = bars.getClose(startBar);
            float scripEnd = bars.getClose(i);
            float compStart = compBars.getClose(compStartIdx);
            float compEnd = compBars.getClose(compEndIdx);
            if (scripStart <= 0 || compStart <= 0) continue;
            double logValue = Math.log((scripEnd / scripStart) / (compEnd / compStart));
            boolean scripUp = scripEnd >= scripStart;
            boolean compUp = compEnd >= compStart;
            double[] target;
            if (scripUp == compUp) {
                target = sync;
            } else if (scripUp) {
                target = posDiv;
            } else {
                target = negDiv;
            }
            target[i] = logValue;
            if (i > period && !Double.isNaN(sync[i - 1])) {
                target[i - 1] = sync[i - 1];
            } else if (i > period && !Double.isNaN(posDiv[i - 1])) {
                target[i - 1] = posDiv[i - 1];
            } else if (i > period && !Double.isNaN(negDiv[i - 1])) {
                target[i - 1] = negDiv[i - 1];
            }
        }
    }

    private int[] buildCompIndexMap(Bars bars, Bars compBars) {
        int size = bars.size();
        int[] map = new int[size];
        int compIdx = 0;
        for (int i = 0; i < size; i++) {
            long ts = bars.getTimestamp(i);
            while (compIdx < compBars.size() && compBars.getTimestamp(compIdx) < ts) {
                compIdx++;
            }
            if (compIdx < compBars.size() && compBars.getTimestamp(compIdx) == ts) {
                map[i] = compIdx;
            } else {
                map[i] = -1;
            }
        }
        return map;
    }

    private SubChartResult buildResult(double[] sync, double[] posDiv, double[] negDiv, String compScripId, int period) {
        String label = INDICATOR_NAME + "(" + compScripId + ", " + period + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(posDiv, POSITIVE_DIVERGENCE_COLOR),
                new SubChartResult.ExtraSeries(negDiv, NEGATIVE_DIVERGENCE_COLOR));
        return new SubChartResult(sync, SYNC_COLOR, label, true, extras, null, null, null);
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
    }

    private String resolveString(Map<String, Object> settings, String key, String defaultValue) {
        Object val = settings.get(key);
        if (val instanceof String s && !s.isBlank()) return s;
        return defaultValue;
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer i) return i;
        return defaultValue;
    }

}
