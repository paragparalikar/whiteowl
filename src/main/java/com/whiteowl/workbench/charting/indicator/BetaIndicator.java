package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.indicator.IndicatorFunctions;
import com.whiteowl.core.scrip.model.Scrip;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public final class BetaIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Beta";
    private static final String SETTING_BENCHMARK = "Benchmark";
    private static final String SETTING_PERIOD = "Period";
    private static final String DEFAULT_BENCHMARK = "NSE:NIFTY 50";
    private static final int DEFAULT_PERIOD = 252;
    private static final double BASELINE = 1.0;
    private static final Color BETA_COLOR = Color.web("#e67e22");

    private final BarsRepository barsRepository;

    public BetaIndicator(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(SETTING_BENCHMARK, Scrip.class, DEFAULT_BENCHMARK),
                new IndicatorSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        String benchmarkId = resolveString(settings, SETTING_BENCHMARK, DEFAULT_BENCHMARK);
        int period = resolveInt(settings, SETTING_PERIOD, DEFAULT_PERIOD);
        int size = bars.size();
        double[] result = new double[size];
        initializeEmpty(result, size);
        Bars benchmarkBars = loadBenchmarkBars(benchmarkId, bars.getTimeframe());
        if (benchmarkBars == null || benchmarkBars.size() == 0) {
            return buildResult(result, benchmarkId, period);
        }
        float[] stockClose = bars.arrays().close();
        float[] benchClose = alignBenchmark(bars, benchmarkBars);
        if (benchClose == null) {
            return buildResult(result, benchmarkId, period);
        }
        float[] beta = IndicatorFunctions.beta(stockClose, benchClose, size, period);
        for (int i = 0; i < size; i++) {
            result[i] = Float.isNaN(beta[i]) ? Double.NaN : beta[i];
        }
        return buildResult(result, benchmarkId, period);
    }

    private float[] alignBenchmark(Bars bars, Bars benchmarkBars) {
        int size = bars.size();
        float[] aligned = new float[size];
        int bmkIdx = 0;
        int bmkSize = benchmarkBars.size();
        for (int i = 0; i < size; i++) {
            long ts = bars.getTimestamp(i);
            while (bmkIdx < bmkSize && benchmarkBars.getTimestamp(bmkIdx) < ts) {
                bmkIdx++;
            }
            if (bmkIdx < bmkSize && benchmarkBars.getTimestamp(bmkIdx) == ts) {
                aligned[i] = benchmarkBars.getClose(bmkIdx);
            } else if (bmkIdx > 0) {
                aligned[i] = benchmarkBars.getClose(bmkIdx - 1);
            } else {
                return null;
            }
        }
        return aligned;
    }

    private Bars loadBenchmarkBars(String scripId, Timeframe timeframe) {
        try {
            Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
            int count = barsRepository.countBars(scripId, loadTf);
            if (count == 0) return null;
            return barsRepository.loadRange(scripId, loadTf, 0, count);
        } catch (IOException e) {
            log.error("Failed to load benchmark bars for {}", scripId, e);
            return null;
        }
    }

    private SubChartResult buildResult(double[] values, String benchmarkId, int period) {
        String label = INDICATOR_NAME + "(" + benchmarkId + ", " + period + ")";
        return new SubChartResult(values, BETA_COLOR, label, true, List.of(),
                null, null, null, BASELINE);
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
