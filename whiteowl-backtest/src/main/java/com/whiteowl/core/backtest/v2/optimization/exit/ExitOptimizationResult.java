package com.whiteowl.core.backtest.v2.optimization.exit;

import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;

import java.util.List;

/**
 * Outcome of exit optimization for one entry configuration.
 *
 * @param entryCombination the entry-side parameters this exit was tuned for
 * @param excursions       raw-trade MAE/MFE population (max time stop run)
 * @param evaluated        per-exit-policy metrics keyed by pseudo combination
 * @param selection        plateau-based selection over the exit dimensions
 * @param policy           the selected standardized exit policy
 * @param metrics          metrics of {@code policy} on the entry configuration
 */
public record ExitOptimizationResult(
        ParameterCombination entryCombination,
        List<Excursion> excursions,
        List<com.whiteowl.core.backtest.v2.optimization.OptimizationResult> evaluated,
        SelectionResult selection,
        StandardExitPolicy policy,
        OptimizationMetrics metrics) {
}
