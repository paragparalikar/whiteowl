package com.whiteowl.core.backtest.v2.optimization;

/** Progress callback invoked after each combination finishes (any thread). */
@FunctionalInterface
public interface OptimizationProgressListener {

    void onProgress(int completed, int total, OptimizationResult latest);

}
