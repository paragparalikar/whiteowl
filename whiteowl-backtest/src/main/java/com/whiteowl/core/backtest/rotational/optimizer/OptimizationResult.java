package com.whiteowl.core.backtest.rotational.optimizer;

import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.rotational.RotationalMetrics;

/**
 * Result of a single optimization run (one config point).
 *
 * @param config  the config used for this run
 * @param metrics the resulting metrics (null if the run failed)
 * @param paramValues values of the optimized parameters for this run
 */
public record OptimizationResult(
        RotationalBacktestConfig config,
        RotationalMetrics metrics,
        double[] paramValues
) {}
