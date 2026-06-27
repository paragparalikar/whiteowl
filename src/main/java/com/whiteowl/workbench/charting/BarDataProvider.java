package com.whiteowl.workbench.charting;

import com.whiteowl.core.bar.aggregation.BarsAggregator;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public final class BarDataProvider {

    private static final double BUFFER_RATIO = 0.5;

    private final BarsRepository barsRepository;
    private String scripId;
    private Timeframe timeframe;
    private Bars cachedBars;
    private int cachedFrom;
    private int cachedTo;
    private int totalBars;
    private int maxBars = -1;
    private Bars aggregatedBars;

    public BarDataProvider(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    public int configure(String scripId, Timeframe timeframe) throws IOException {
        this.scripId = scripId;
        this.timeframe = timeframe;
        this.maxBars = -1;
        invalidateCache();
        if (timeframe.isAggregated()) {
            return configureAggregated(scripId, timeframe);
        }
        this.totalBars = barsRepository.countBars(scripId, timeframe);
        return totalBars;
    }

    private int configureAggregated(String scripId, Timeframe timeframe) throws IOException {
        Timeframe source = timeframe.getSourceTimeframe();
        int sourceTotalBars = barsRepository.countBars(scripId, source);
        if (sourceTotalBars == 0) {
            this.totalBars = 0;
            return 0;
        }
        Bars sourceBars = barsRepository.loadRange(scripId, source, 0, sourceTotalBars);
        this.aggregatedBars = BarsAggregator.aggregate(sourceBars, timeframe);
        this.totalBars = aggregatedBars.size();
        log.debug("Aggregated {} {} bars into {} {} bars for {}", sourceTotalBars, source, totalBars, timeframe, scripId);
        return totalBars;
    }

    public void setMaxBars(int maxBars) {
        this.maxBars = maxBars;
    }

    public void clearMaxBars() {
        this.maxBars = -1;
    }

    public Bars fetchBars(int viewStart, int viewEnd) throws IOException {
        if (timeframe.isAggregated()) {
            return fetchAggregatedBars(viewStart, viewEnd);
        }
        int effectiveBound = maxBars > 0 ? maxBars : totalBars;
        int effectiveStart = Math.max(0, viewStart);
        int effectiveEnd = Math.min(viewEnd, effectiveBound);
        if (cachedBars != null && effectiveStart >= cachedFrom && effectiveEnd <= cachedTo) {
            return cachedBars;
        }
        int visibleCount = viewEnd - viewStart;
        int buffer = (int) (visibleCount * BUFFER_RATIO);
        int fetchFrom = Math.max(0, effectiveStart - buffer);
        int fetchTo = Math.min(viewEnd + buffer, effectiveBound);
        cachedBars = barsRepository.loadRange(scripId, timeframe, fetchFrom, fetchTo);
        cachedFrom = fetchFrom;
        cachedTo = fetchFrom + cachedBars.size();
        log.debug("Fetched bars [{}, {}) for {} {}", cachedFrom, cachedTo, scripId, timeframe);
        return cachedBars;
    }

    private Bars fetchAggregatedBars(int viewStart, int viewEnd) {
        if (aggregatedBars == null) return null;
        int effectiveBound = maxBars > 0 ? maxBars : totalBars;
        int effectiveStart = Math.max(0, viewStart);
        int effectiveEnd = Math.min(viewEnd, effectiveBound);
        if (cachedBars != null && effectiveStart >= cachedFrom && effectiveEnd <= cachedTo) {
            return cachedBars;
        }
        int visibleCount = viewEnd - viewStart;
        int buffer = (int) (visibleCount * BUFFER_RATIO);
        int fetchFrom = Math.max(0, effectiveStart - buffer);
        int fetchTo = Math.min(viewEnd + buffer, effectiveBound);
        Bars slice = new Bars(aggregatedBars.getScripId(), timeframe, fetchTo - fetchFrom);
        for (int i = fetchFrom; i < fetchTo && i < aggregatedBars.size(); i++) {
            slice.append(aggregatedBars.getTimestamp(i), aggregatedBars.getOpen(i),
                    aggregatedBars.getHigh(i), aggregatedBars.getLow(i),
                    aggregatedBars.getClose(i), aggregatedBars.getVolume(i));
        }
        cachedBars = slice;
        cachedFrom = fetchFrom;
        cachedTo = fetchFrom + slice.size();
        return cachedBars;
    }

    public Bars peekBars() {
        return cachedBars;
    }

    public int translateIndex(int viewIndex) {
        return viewIndex - cachedFrom;
    }

    public int getCachedFrom() {
        return cachedFrom;
    }

    public void invalidateCache() {
        cachedBars = null;
        cachedFrom = 0;
        cachedTo = 0;
        aggregatedBars = null;
    }

}
