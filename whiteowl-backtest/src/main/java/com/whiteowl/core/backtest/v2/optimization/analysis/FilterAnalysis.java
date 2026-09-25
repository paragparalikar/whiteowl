package com.whiteowl.core.backtest.v2.optimization.analysis;

import java.util.List;

/**
 * Outcome of univariate filter analysis for one feature.
 *
 * @param feature       feature name (e.g. "adx", "rsi")
 * @param bins          per-bin statistics over the feature range
 * @param accepted      whether a qualifying stable profitable region exists
 * @param regionLower   accepted region lower bound (NaN if rejected)
 * @param regionUpper   accepted region upper bound (NaN if rejected)
 * @param improvement   region mean metric minus baseline metric
 * @param reason        audit-trail explanation
 */
public record FilterAnalysis(
        String feature,
        List<BucketStats> bins,
        boolean accepted,
        double regionLower,
        double regionUpper,
        double improvement,
        String reason) {
}
