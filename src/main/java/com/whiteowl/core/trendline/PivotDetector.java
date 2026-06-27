package com.whiteowl.core.trendline;

import com.whiteowl.core.bar.model.Bars;

import java.util.ArrayList;
import java.util.List;

public final class PivotDetector {

    private PivotDetector() {
    }

    public static List<Pivot> findPivotHighs(Bars bars, int lookback) {
        return findPivotHighs(bars, lookback, false);
    }

    public static List<Pivot> findPivotHighs(Bars bars, int lookback, boolean useClose) {
        List<Pivot> pivots = new ArrayList<>();
        int size = bars.size();
        for (int i = lookback; i < size - lookback; i++) {
            if (shouldSkipBar(bars, i)) continue;
            float candidate = useClose ? bars.getClose(i) : bars.getHigh(i);
            if (isLocalMax(bars, i, lookback, candidate, useClose)) {
                pivots.add(new Pivot(i, candidate));
            }
        }
        return pivots;
    }

    public static List<Pivot> findPivotLows(Bars bars, int lookback) {
        return findPivotLows(bars, lookback, false);
    }

    public static List<Pivot> findPivotLows(Bars bars, int lookback, boolean useClose) {
        List<Pivot> pivots = new ArrayList<>();
        int size = bars.size();
        for (int i = lookback; i < size - lookback; i++) {
            if (shouldSkipBar(bars, i)) continue;
            float candidate = useClose ? bars.getClose(i) : bars.getLow(i);
            if (isLocalMin(bars, i, lookback, candidate, useClose)) {
                pivots.add(new Pivot(i, candidate));
            }
        }
        return pivots;
    }

    private static boolean shouldSkipBar(Bars bars, int index) {
        return Float.compare(bars.getOpen(index), bars.getHigh(index)) == 0;
    }

    private static boolean isLocalMax(Bars bars, int center, int lookback, float candidate, boolean useClose) {
        for (int j = center - lookback; j <= center + lookback; j++) {
            if (j == center) continue;
            float compare = useClose ? bars.getClose(j) : bars.getHigh(j);
            if (compare >= candidate) return false;
        }
        return true;
    }

    private static boolean isLocalMin(Bars bars, int center, int lookback, float candidate, boolean useClose) {
        for (int j = center - lookback; j <= center + lookback; j++) {
            if (j == center) continue;
            float compare = useClose ? bars.getClose(j) : bars.getLow(j);
            if (compare <= candidate) return false;
        }
        return true;
    }

}
