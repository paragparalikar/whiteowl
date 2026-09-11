package com.whiteowl.core.rs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Pre-computed equal-weight composite of a group.
 * Reused across RS computations for different target scrips within the same group.
 */
@Getter
@RequiredArgsConstructor
public final class GroupComposite {
    private final long[] timestamps;            // union timeline
    private final float[] compositeClose;       // equal-weight rebased average at each bar
    private final int size;
    private final int scripCount;               // number of scrips in the composite

    /**
     * Per-scrip rebased closes aligned to the union timeline.
     * Dimensions: [scripIndex][timelineIndex].
     * Value is NaN when a scrip has no data at that timestamp.
     * Needed by RSRank to compute individual returns for ranking.
     */
    private final float[][] perScripRebased;

    /**
     * Scrip IDs in the same order as perScripRebased rows.
     */
    private final String[] scripIds;
}
