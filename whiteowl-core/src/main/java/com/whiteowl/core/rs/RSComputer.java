package com.whiteowl.core.rs;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class RSComputer {

    private final BarsRepository barsRepository;

    // Level 1 cache: group composite (shared across all target scrips)
    private final Map<CompositeKey, GroupComposite> compositeCache = new ConcurrentHashMap<>();

    // Level 2 cache: per-scrip RS result
    private final Map<RSCacheKey, RSResult> rsCache = new ConcurrentHashMap<>();

    public RSComputer(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
    }

    /**
     * Compute RS for a target scrip against a group.
     *
     * @param targetScripId  the scrip whose RS is being computed (the charted scrip)
     * @param groupName      name of the group (for cache key)
     * @param groupScripIds  scrip IDs in the group
     * @param timeframe      chart timeframe
     * @param formula        which RS formula to apply
     * @param period         lookback period (formula-specific)
     * @return RSResult with timestamps and RS values
     */
    public RSResult compute(String targetScripId, String groupName,
                            List<String> groupScripIds, Timeframe timeframe,
                            RSFormula formula, int period) {

        RSCacheKey rsKey = new RSCacheKey(targetScripId, groupName, formula.name(), timeframe, period);
        RSResult cached = rsCache.get(rsKey);
        if (cached != null) return cached;

        // Step 1: Get or build the group composite
        GroupComposite composite = getOrBuildComposite(groupName, groupScripIds, timeframe);
        if (composite.getSize() < 2) {
            return emptyResult();
        }

        // Step 2: Load and align the target scrip's bars to the composite timeline
        float[] targetRebased = buildTargetRebased(targetScripId, timeframe, composite);

        // Step 3: Find target's index in the composite (for RSRank)
        int targetIndex = findTargetIndex(targetScripId, composite);

        // Step 4: Run the formula
        float[] values = formula.compute(targetRebased, composite, targetIndex, period);

        RSResult result = new RSResult(
                composite.getTimestamps(), values, composite.getSize(), composite.getScripCount());

        rsCache.put(rsKey, result);
        return result;
    }

    /**
     * Invalidate cache for a specific group (call when group composition changes).
     */
    public void invalidate(String groupName) {
        compositeCache.keySet().removeIf(k -> k.groupName.equals(groupName));
        rsCache.keySet().removeIf(k -> k.groupName.equals(groupName));
    }

    /**
     * Invalidate all cached results (call when new bar data is downloaded).
     */
    public void invalidateAll() {
        compositeCache.clear();
        rsCache.clear();
    }

    // --- Private helpers ---

    private GroupComposite getOrBuildComposite(String groupName, List<String> scripIds,
                                               Timeframe timeframe) {
        CompositeKey key = new CompositeKey(groupName, timeframe);
        GroupComposite cached = compositeCache.get(key);
        if (cached != null) return cached;

        GroupComposite composite = buildComposite(scripIds, timeframe);
        compositeCache.put(key, composite);
        return composite;
    }

    private GroupComposite buildComposite(List<String> scripIds, Timeframe timeframe) {
        Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
        List<Bars> allBars = new ArrayList<>();
        List<String> loadedIds = new ArrayList<>();
        for (String scripId : scripIds) {
            try {
                Bars bars = barsRepository.load(scripId, loadTf);
                if (bars != null && bars.size() > 0) {
                    allBars.add(bars);
                    loadedIds.add(scripId);
                }
            } catch (IOException e) {
                log.debug("Failed to load bars for {}: {}", scripId, e.getMessage());
            }
        }
        if (allBars.isEmpty()) {
            return new GroupComposite(new long[0], new float[0], 0, 0,
                    new float[0][], new String[0]);
        }

        // Build union timeline
        TreeSet<Long> union = new TreeSet<>();
        for (Bars bars : allBars) {
            for (int i = 0; i < bars.size(); i++) {
                union.add(bars.getTimestamp(i));
            }
        }
        long[] timeline = union.stream().mapToLong(Long::longValue).toArray();
        int timelineLen = timeline.length;
        int scripCount = allBars.size();

        // Build per-scrip rebased arrays
        float[][] perScripRebased = new float[scripCount][timelineLen];
        for (int s = 0; s < scripCount; s++) {
            Bars bars = allBars.get(s);
            float firstClose = Float.NaN;
            int barIdx = 0;
            for (int t = 0; t < timelineLen; t++) {
                long target = timeline[t];
                while (barIdx < bars.size() && bars.getTimestamp(barIdx) < target) {
                    barIdx++;
                }
                if (barIdx < bars.size() && bars.getTimestamp(barIdx) == target) {
                    float close = bars.getClose(barIdx);
                    if (Float.isNaN(firstClose)) {
                        firstClose = close;
                    }
                    perScripRebased[s][t] = (firstClose > 0) ? (close / firstClose) * 100f : Float.NaN;
                } else {
                    perScripRebased[s][t] = Float.NaN;
                }
            }
        }

        // Build composite: equal-weight average of rebased closes at each timestamp
        float[] compositeClose = new float[timelineLen];
        for (int t = 0; t < timelineLen; t++) {
            float sum = 0;
            int count = 0;
            for (int s = 0; s < scripCount; s++) {
                if (!Float.isNaN(perScripRebased[s][t])) {
                    sum += perScripRebased[s][t];
                    count++;
                }
            }
            compositeClose[t] = count > 0 ? sum / count : Float.NaN;
        }

        return new GroupComposite(timeline, compositeClose, timelineLen, scripCount,
                perScripRebased, loadedIds.toArray(new String[0]));
    }

    private float[] buildTargetRebased(String targetScripId, Timeframe timeframe,
                                        GroupComposite composite) {
        long[] timeline = composite.getTimestamps();
        int size = composite.getSize();

        // If target is in the group, reuse the pre-computed rebased array
        int targetIndex = findTargetIndex(targetScripId, composite);
        if (targetIndex >= 0) {
            return composite.getPerScripRebased()[targetIndex];
        }

        // Target is not in the group — load and rebase separately
        float[] rebased = new float[size];
        Arrays.fill(rebased, Float.NaN);
        try {
            Timeframe loadTf = timeframe.isAggregated() ? timeframe.getSourceTimeframe() : timeframe;
            Bars bars = barsRepository.load(targetScripId, loadTf);
            if (bars == null || bars.size() == 0) return rebased;

            float firstClose = Float.NaN;
            int barIdx = 0;
            for (int t = 0; t < size; t++) {
                long target = timeline[t];
                while (barIdx < bars.size() && bars.getTimestamp(barIdx) < target) {
                    barIdx++;
                }
                if (barIdx < bars.size() && bars.getTimestamp(barIdx) == target) {
                    float close = bars.getClose(barIdx);
                    if (Float.isNaN(firstClose)) firstClose = close;
                    rebased[t] = (firstClose > 0) ? (close / firstClose) * 100f : Float.NaN;
                }
            }
        } catch (IOException e) {
            log.debug("Failed to load target bars for {}: {}", targetScripId, e.getMessage());
        }
        return rebased;
    }

    private int findTargetIndex(String targetScripId, GroupComposite composite) {
        String[] ids = composite.getScripIds();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(targetScripId)) return i;
        }
        return -1;
    }

    private RSResult emptyResult() {
        return new RSResult(new long[0], new float[0], 0, 0);
    }

    private record CompositeKey(String groupName, Timeframe timeframe) {}
    private record RSCacheKey(String targetScripId, String groupName,
                              String formulaName, Timeframe timeframe, int period) {}
}
