package com.whiteowl.core.breadth;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class BreadthComputer {

    private final BarsRepository barsRepository;
    private final Map<CacheKey, BreadthResult> cache = new ConcurrentHashMap<>();

    public BreadthComputer(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    /**
     * Compute breadth for a list of scrip IDs at a given timeframe using the specified formula.
     * Uses a span of 1 (compare each bar to the immediately previous bar).
     */
    public BreadthResult compute(String groupName, List<String> scripIds,
                                  Timeframe timeframe, BreadthFormula formula) {
        return compute(groupName, scripIds, timeframe, formula, 1);
    }

    /**
     * Compute breadth for a list of scrip IDs at a given timeframe using the specified formula.
     *
     * @param span lookback period: each bar's close is compared to the close {@code span} bars ago.
     *             span=1 is bar-by-bar; span=10 compares against 10 bars ago.
     */
    public BreadthResult compute(String groupName, List<String> scripIds,
                                  Timeframe timeframe, BreadthFormula formula, int span) {
        int effectiveSpan = Math.max(1, span);
        CacheKey key = new CacheKey(groupName, formula.name(), timeframe, effectiveSpan);
        BreadthResult cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        List<Bars> allBars = loadAllBars(scripIds, timeframe);
        if (allBars.isEmpty()) {
            return emptyResult();
        }

        long[] masterTimeline = buildUnionTimeline(allBars);
        if (masterTimeline.length < effectiveSpan + 1) {
            return emptyResult();
        }

        int[][] indexMaps = buildIndexMaps(allBars, masterTimeline);

        int timelineLen = masterTimeline.length;
        int scripCount = allBars.size();
        float[] positive = new float[timelineLen];
        float[] negative = new float[timelineLen];
        float[] net = new float[timelineLen];

        // Reusable scratch arrays
        float[] closes = new float[scripCount];
        float[] prevCloses = new float[scripCount];
        long[] volumes = new long[scripCount];

        // Bars before span have no lookback reference — set to NaN
        for (int t = 0; t < effectiveSpan; t++) {
            positive[t] = Float.NaN;
            negative[t] = Float.NaN;
            net[t] = Float.NaN;
        }

        for (int t = effectiveSpan; t < timelineLen; t++) {
            int validCount = 0;
            for (int s = 0; s < scripCount; s++) {
                int idx = indexMaps[s][t];
                int prevIdx = indexMaps[s][t - effectiveSpan];
                if (idx < 0 || prevIdx < 0) continue;
                Bars bars = allBars.get(s);
                closes[validCount] = bars.getClose(idx);
                prevCloses[validCount] = bars.getClose(prevIdx);
                volumes[validCount] = bars.getVolume(idx);
                validCount++;
            }
            if (validCount == 0) {
                positive[t] = Float.NaN;
                negative[t] = Float.NaN;
                net[t] = Float.NaN;
                continue;
            }
            BreadthBar bar = formula.compute(closes, prevCloses, volumes, validCount);
            positive[t] = bar.positive();
            negative[t] = bar.negative();
            net[t] = bar.net();
        }

        BreadthResult result = new BreadthResult(
                masterTimeline, positive, negative, net, timelineLen, scripCount);

        cache.put(key, result);
        return result;
    }

    /**
     * Invalidate cache for a specific group (call when group composition changes).
     */
    public void invalidate(String groupName) {
        cache.keySet().removeIf(k -> k.groupName.equals(groupName));
    }

    /**
     * Invalidate all cached results (call when new bar data is downloaded).
     */
    public void invalidateAll() {
        cache.clear();
    }

    // --- Private helpers ---

    private List<Bars> loadAllBars(List<String> scripIds, Timeframe timeframe) {
        Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
        List<Bars> result = new ArrayList<>();
        for (String scripId : scripIds) {
            try {
                Bars bars = barsRepository.load(scripId, loadTf);
                if (bars != null && bars.size() > 0) {
                    result.add(bars);
                }
            } catch (IOException e) {
                log.debug("Failed to load bars for {} at {}: {}", scripId, loadTf, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Build union timeline: all timestamps present in ANY scrip.
     * At each timestamp, breadth is computed from whichever scrips have data
     * (the formula loop skips scrips with missing bars via the idx < 0 check).
     * This avoids truncating the timeline to the shortest-history scrip.
     */
    private long[] buildUnionTimeline(List<Bars> allBars) {
        TreeSet<Long> union = new TreeSet<>();
        for (Bars bars : allBars) {
            for (int i = 0; i < bars.size(); i++) {
                union.add(bars.getTimestamp(i));
            }
        }
        return union.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * For each scrip, build a map from master timeline index to scrip bar index.
     * Linear scan forward since both arrays are sorted.
     */
    private int[][] buildIndexMaps(List<Bars> allBars, long[] masterTimeline) {
        int scripCount = allBars.size();
        int timelineLen = masterTimeline.length;
        int[][] maps = new int[scripCount][timelineLen];

        for (int s = 0; s < scripCount; s++) {
            Bars bars = allBars.get(s);
            int barIdx = 0;
            for (int t = 0; t < timelineLen; t++) {
                long target = masterTimeline[t];
                while (barIdx < bars.size() && bars.getTimestamp(barIdx) < target) {
                    barIdx++;
                }
                if (barIdx < bars.size() && bars.getTimestamp(barIdx) == target) {
                    maps[s][t] = barIdx;
                } else {
                    maps[s][t] = -1;
                }
            }
        }
        return maps;
    }

    private BreadthResult emptyResult() {
        return new BreadthResult(new long[0], new float[0], new float[0], new float[0], 0, 0);
    }

    private record CacheKey(String groupName, String formulaName, Timeframe timeframe, int span) {}
}
