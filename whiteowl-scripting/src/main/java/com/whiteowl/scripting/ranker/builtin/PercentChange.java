package com.whiteowl.scripting.ranker.builtin;

import com.whiteowl.core.bar.model.Bars;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PercentChange {

    /**
     * Percentage change between the close {@code period} bars ago and the latest close.
     * Returns {@code null} if there aren't enough bars or the reference close is non-positive.
     */
    static Double compute(Bars bars, int period) {
        int size = bars.size();
        if (size < period + 1) return null;
        float first = bars.getClose(size - 1 - period);
        float last = bars.getClose(size - 1);
        if (first <= 0f || Float.isNaN(first) || Float.isNaN(last)) return null;
        return ((double) last - first) / first * 100.0;
    }

}
