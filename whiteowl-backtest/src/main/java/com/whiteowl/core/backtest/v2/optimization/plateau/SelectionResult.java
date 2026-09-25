package com.whiteowl.core.backtest.v2.optimization.plateau;

import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;

import java.util.List;
import java.util.Map;

/**
 * Result of robust parameter selection over one result set (one timeframe).
 *
 * @param perParameter    plateau analysis per optimizable parameter
 * @param plateaus        plateau outcome per parameter (for reporting)
 * @param combination     the selected combination (inside all plateaus when
 *                        possible)
 * @param selectedResult  the grid result backing {@link #combination}
 * @param warnings        warnings raised during selection
 */
public record SelectionResult(
        Map<String, ParameterSelection> perParameter,
        Map<String, PlateauResult> plateaus,
        ParameterCombination combination,
        OptimizationResult selectedResult,
        List<ResearchWarning> warnings) {
}
