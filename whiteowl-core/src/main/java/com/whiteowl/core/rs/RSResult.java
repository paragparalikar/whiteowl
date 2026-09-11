package com.whiteowl.core.rs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Result of an RS computation for a single scrip against a group.
 */
@Getter
@RequiredArgsConstructor
public final class RSResult {
    private final long[] timestamps;    // aligned timeline
    private final float[] values;       // RS values per bar
    private final int size;             // number of bars
    private final int groupScripCount;  // how many scrips contributed to the composite
}
