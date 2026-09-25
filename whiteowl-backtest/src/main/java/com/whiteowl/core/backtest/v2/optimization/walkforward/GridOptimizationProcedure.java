package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ParameterConstraint;
import com.whiteowl.core.backtest.v2.optimization.ParameterGrid;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.backtest.v2.optimization.plateau.RobustParameterSelector;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.bar.model.Timeframe;

import java.util.List;

/**
 * Default in-sample optimization procedure: full parameter grid → per-combo
 * backtest metrics → plateau-based robust selection. Composes Phase 1 and
 * Phase 2 into the unit of work the walk-forward engine repeats per window.
 */
public final class GridOptimizationProcedure implements OptimizationProcedure {

    private final StrategySpec strategy;
    private final List<StrategyInput> inputs;
    private final List<ParameterConstraint> constraints;
    private final Timeframe timeframe;
    private final com.whiteowl.core.backtest.v2.engine.StandardExitPolicy exits;
    private final ResearchConfiguration config;
    private final OptimizationEngine engine;
    private final RobustParameterSelector selector;

    public GridOptimizationProcedure(StrategySpec strategy, List<StrategyInput> inputs,
                                      List<ParameterConstraint> constraints,
                                      Timeframe timeframe,
                                      com.whiteowl.core.backtest.v2.engine.StandardExitPolicy exits,
                                      ResearchConfiguration config,
                                      OptimizationEngine engine) {
        this.strategy = strategy;
        this.inputs = inputs;
        this.constraints = constraints == null ? List.of() : constraints;
        this.timeframe = timeframe;
        this.exits = exits;
        this.config = config;
        this.engine = engine;
        this.selector = new RobustParameterSelector(config);
    }

    @Override
    public ProcedureResult optimize(List<ScripSlice> inSampleSlices) {
        List<ParameterCombination> grid = ParameterGrid.generate(
                inputs, constraints, config.getMaxParameterCombinations());
        List<OptimizationResult> results = engine.evaluateGrid(
                strategy, grid, inSampleSlices, timeframe, exits, null, null);
        SelectionResult selection = selector.select(results, inputs);
        return new ProcedureResult(selection.combination(),
                selection.selectedResult().metrics());
    }

}
