package com.whiteowl.core.backtest.v2.optimization.plateau;

import java.util.List;

/**
 * A contiguous, stable region of good performance on a parameter curve —
 * the preferred alternative to an isolated optimum.
 *
 * @param lowerBound      smallest parameter value in the region
 * @param upperBound      largest parameter value in the region
 * @param points          region members (sorted by parameter value)
 * @param meanMetric      mean of raw metrics inside the region
 * @param stdDevMetric    standard deviation of raw metrics inside the region
 * @param minTradeCount   smallest trade count among region members
 * @param robustnessScore composite score — see {@link RobustnessScore}
 */
public record PlateauRegion(
        double lowerBound,
        double upperBound,
        List<CurvePoint> points,
        double meanMetric,
        double stdDevMetric,
        int minTradeCount,
        double robustnessScore) {

    public int size() {
        return points.size();
    }

    public boolean contains(double value) {
        return value >= lowerBound && value <= upperBound;
    }

}
