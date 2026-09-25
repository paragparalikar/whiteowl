package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;

/**
 * What an {@link OptimizationProcedure} returns for one in-sample window.
 *
 * @param combination     selected (robust) parameter combination
 * @param inSampleMetrics metrics of that combination on the in-sample window
 */
public record ProcedureResult(ParameterCombination combination,
                              OptimizationMetrics inSampleMetrics) {
}
