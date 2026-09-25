package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;

/**
 * One walk-forward iteration.
 *
 * @param windowIndex     0-based window number
 * @param trainStart      in-sample window start (epoch millis)
 * @param trainEnd        in-sample window end / OOS window start
 * @param testEnd         OOS window end
 * @param combination     parameters selected on the in-sample window (null on failure)
 * @param inSample        metrics of the selected combination in-sample
 * @param outOfSample     metrics of the frozen combination on unseen data
 * @param errorMessage    non-null if the window failed
 */
public record WalkForwardWindowResult(
        int windowIndex,
        long trainStart,
        long trainEnd,
        long testEnd,
        ParameterCombination combination,
        OptimizationMetrics inSample,
        OptimizationMetrics outOfSample,
        String errorMessage) {

    public boolean failed() {
        return errorMessage != null;
    }

}
