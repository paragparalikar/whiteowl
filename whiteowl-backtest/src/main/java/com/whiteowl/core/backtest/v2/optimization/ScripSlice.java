package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.bar.model.BarsArrays;

/**
 * A per-scrip bar window handed to the engine.
 *
 * <p>{@code arrays} may include warmup bars before the evaluation window;
 * {@code evalStartIndex}/{@code evalEndIndex} delimit the portion that counts:
 * only trades whose {@code entryBarIndex} falls inside
 * {@code [evalStartIndex, evalEndIndex)} are included in metrics, and the
 * equity curve is trimmed to the same range. This lets indicators warm up on
 * pre-window data without contaminating window statistics.</p>
 */
public record ScripSlice(String scripId, BarsArrays arrays,
                         int evalStartIndex, int evalEndIndex) {

    public ScripSlice(String scripId, BarsArrays arrays) {
        this(scripId, arrays, 0, arrays.size());
    }

}
