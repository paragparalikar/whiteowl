package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.bar.model.Timeframe;

/**
 * Outcome of evaluating one (timeframe, parameter combination) pair across all
 * requested instruments. A failed task never aborts the run — it is recorded
 * here with status {@link Status#FAILED}.
 */
public record OptimizationResult(
        Timeframe timeframe,
        ParameterCombination combination,
        OptimizationMetrics metrics,
        Status status,
        String errorMessage) {

    public enum Status {OK, INSUFFICIENT_SAMPLE, FAILED}

    public static OptimizationResult ok(Timeframe tf, ParameterCombination c,
                                         OptimizationMetrics m, int minTrades) {
        Status status = m.totalTrades() < minTrades ? Status.INSUFFICIENT_SAMPLE : Status.OK;
        return new OptimizationResult(tf, c, m, status, null);
    }

    public static OptimizationResult failed(Timeframe tf, ParameterCombination c, String error) {
        return new OptimizationResult(tf, c, null, Status.FAILED, error);
    }

}
