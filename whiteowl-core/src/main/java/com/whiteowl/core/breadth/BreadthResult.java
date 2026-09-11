package com.whiteowl.core.breadth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Complete breadth computation result for a group across all bars.
 */
@Getter
@RequiredArgsConstructor
public final class BreadthResult {
    private final long[] timestamps;    // aligned master timeline
    private final float[] positive;     // positive component per bar
    private final float[] negative;     // negative component per bar
    private final float[] net;          // net = positive - negative per bar
    private final int size;             // number of bars
    private final int scripCount;       // number of scrips in the group that had data
}
