package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.bar.model.BarsArrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * Index/timestamp helpers for cutting {@link BarsArrays} into windows without
 * mutating the shared source arrays.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BarsSlicer {

    /** Returns a new BarsArrays covering indices {@code [from, toExclusive)}. */
    public static BarsArrays subrange(BarsArrays a, int from, int toExclusive) {
        from = Math.max(0, from);
        toExclusive = Math.min(a.size(), toExclusive);
        if (toExclusive <= from) {
            return new BarsArrays(new float[0], new float[0], new float[0],
                    new float[0], new long[0], new long[0], 0);
        }
        return new BarsArrays(
                Arrays.copyOfRange(a.open(), from, toExclusive),
                Arrays.copyOfRange(a.high(), from, toExclusive),
                Arrays.copyOfRange(a.low(), from, toExclusive),
                Arrays.copyOfRange(a.close(), from, toExclusive),
                Arrays.copyOfRange(a.volume(), from, toExclusive),
                Arrays.copyOfRange(a.timestamp(), from, toExclusive),
                toExclusive - from);
    }

    /**
     * Index of the first bar whose timestamp is {@code >= ts}, or
     * {@code size} if all bars are earlier.
     */
    public static int firstIndexAtOrAfter(BarsArrays a, long ts) {
        int idx = Arrays.binarySearch(a.timestamp(), 0, a.size(), ts);
        return idx >= 0 ? idx : -idx - 1;
    }

}
