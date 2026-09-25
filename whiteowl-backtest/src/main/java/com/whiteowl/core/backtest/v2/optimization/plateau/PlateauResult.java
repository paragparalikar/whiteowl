package com.whiteowl.core.backtest.v2.optimization.plateau;

import java.util.List;

/**
 * Outcome of plateau analysis for one parameter.
 *
 * @param parameterName    the parameter analyzed
 * @param curve            full curve sorted by parameter value
 * @param region           the chosen stable region, or null if none qualifies
 * @param selectedValue    recommended value (best raw metric inside the
 *                         region; isolated argmax when no region exists)
 * @param globalBestValue  parameter value with the single highest metric
 * @param globalBestMetric the single highest metric on the curve
 * @param stable           whether a qualifying plateau was found
 * @param reason           human-readable explanation of the selection
 */
public record PlateauResult(
        String parameterName,
        List<CurvePoint> curve,
        PlateauRegion region,
        double selectedValue,
        double globalBestValue,
        double globalBestMetric,
        boolean stable,
        String reason) {
}
