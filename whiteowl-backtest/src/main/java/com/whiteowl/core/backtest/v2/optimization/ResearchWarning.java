package com.whiteowl.core.backtest.v2.optimization;

/**
 * Research warnings. These are never suppressed, regardless of how good the
 * headline metric looks.
 */
public enum ResearchWarning {

    LOW_TRADE_COUNT,
    HIGH_PARAMETER_SENSITIVITY,
    HIGH_COMPLEXITY,
    POOR_OUT_OF_SAMPLE_PERFORMANCE,
    HIGH_WALK_FORWARD_PARAMETER_DRIFT,
    HIGH_TRANSACTION_COST_SENSITIVITY,
    HIGH_DRAWDOWN,
    UNSTABLE_REGIME_PERFORMANCE,
    STATISTICALLY_WEAK_EXPLORATION_RESULT,
    POSSIBLE_OVERFITTING,
    INSUFFICIENT_DATA

}
