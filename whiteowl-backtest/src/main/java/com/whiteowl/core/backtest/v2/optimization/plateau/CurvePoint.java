package com.whiteowl.core.backtest.v2.optimization.plateau;

/**
 * One point on a parameter sensitivity curve: a parameter value, the metric
 * achieved there (usually Sortino), and the trade count behind that metric.
 */
public record CurvePoint(double parameterValue, double metric, int tradeCount) {
}
