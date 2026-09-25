package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.optimization.ScripSlice;

import java.util.List;

/**
 * "Given this in-sample data, return the robust configuration." The
 * walk-forward engine is agnostic to how the optimization is performed — the
 * procedure is plugged in, so richer formulations (exits, filters) can replace
 * the default grid procedure as later phases land.
 */
@FunctionalInterface
public interface OptimizationProcedure {

    ProcedureResult optimize(List<ScripSlice> inSampleSlices) throws Exception;

}
