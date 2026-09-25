package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Request for a grid optimization run: one strategy over one or more
 * instruments and timeframes, evaluated on every generated parameter
 * combination.
 */
@Getter
@Builder
public final class OptimizationRequest {

    /** The strategy (hardcoded class supplier or Groovy script source). */
    private final StrategySpec strategy;
    /** Instruments to aggregate over (usually one, e.g. "NIFTY"). */
    @Singular("scrip")
    private final List<String> scripIds;
    /** Every timeframe is evaluated independently. */
    @Singular("timeframe")
    private final List<Timeframe> timeframes;
    /** Bar source; each (scrip, timeframe) is loaded exactly once per run. */
    private final BarsLoader barsLoader;
    /**
     * Optimizable parameters. If null/empty they are discovered from the
     * strategy via {@link StrategySpec#declaredInputs()}.
     */
    private final List<StrategyInput> parameters;
    /** Validity constraints, e.g. {@code ParameterConstraint.lessThan("fast","slow")}. */
    private final List<ParameterConstraint> constraints;
    /**
     * Optional bar range (e.g. the development split). Null → full loaded
     * history.
     */
    private final DataSplit dataSplit;
    /** Optional progress callback. */
    private final OptimizationProgressListener listener;

}
