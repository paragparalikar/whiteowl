package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;

import java.util.List;
import java.util.Map;

/**
 * Aggregate walk-forward outcome.
 *
 * @param windows                 every iteration, in order
 * @param parameterDrift          per-parameter drift statistics
 * @param meanInSampleSortino     trade-weighted mean IS Sortino across windows
 * @param meanOutOfSampleSortino  trade-weighted mean OOS Sortino across windows
 * @param walkForwardEfficiency   {@code meanOosSortino / meanIsSortino};
 *                                ~1.0 means IS performance generalizes
 * @param totalOosTrades          OOS trades across all windows
 * @param warnings                engine-raised warnings
 */
public record WalkForwardResult(
        List<WalkForwardWindowResult> windows,
        Map<String, ParameterDriftStats> parameterDrift,
        double meanInSampleSortino,
        double meanOutOfSampleSortino,
        double walkForwardEfficiency,
        int totalOosTrades,
        List<ResearchWarning> warnings) {
}
