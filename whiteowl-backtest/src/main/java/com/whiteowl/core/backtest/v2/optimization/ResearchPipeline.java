package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.analysis.AutocorrelationAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.CalendarAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.CostSensitivityAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.DrawdownAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.MonteCarloAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.RMultipleAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.StreakAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.TimeframeSelector;
import com.whiteowl.core.backtest.v2.optimization.analysis.TradeObservation;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitGrid;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizer;
import com.whiteowl.core.backtest.v2.optimization.plateau.RobustParameterSelector;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardConfig;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardEngine;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardResult;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Generic strategy-agnostic research pipeline — runs Phases 1–8 of the
 * research plan end to end against any {@link StrategySpec}:
 *
 * <pre>
 *   1  parameter grid × timeframes → metrics
 *   2  plateau-based robust selection per timeframe
 *   3  walk-forward per timeframe
 *   4  MAE/MFE exit optimization on the best timeframe's selected combo
 *   5  univariate filter analysis on the final trade population
 *   6  timeframe selection (sortino × stability × WFE)
 *   7  exploratory analysis: calendar, streaks, autocorrelation,
 *      R-multiples, drawdown
 *   8  Monte Carlo, cost sensitivity, warnings, final configuration
 * </pre>
 *
 * All artifacts land in the returned {@link OptimizationContext} — the report
 * generator consumes it directly.
 */
@Slf4j
public final class ResearchPipeline {

    private final ResearchConfiguration config;

    public ResearchPipeline(ResearchConfiguration config) {
        this.config = config;
    }

    /**
     * @param strategy    strategy under research
     * @param bars        scripId → (timeframe → bars) — one entry per scrip
     * @param constraints grid constraints (e.g. {@code lessThan("a","b")})
     * @param wf          walk-forward window sizing; null → 4m train / 1m
     *                    test / 1m step
     */
    public OptimizationContext run(StrategySpec strategy,
                                   Map<String, Map<Timeframe, BarsArrays>> bars,
                                   List<ParameterConstraint> constraints,
                                   WalkForwardConfig wf) {
        List<String> scripIds = List.copyOf(bars.keySet());
        List<Timeframe> tfs = bars.values().iterator().next().keySet().stream()
                .sorted().toList();
        OptimizationContext context =
                OptimizationContext.of(strategy.strategyId(), scripIds, tfs, config);
        context.setStrategyName(strategy.name());
        context.getJob().setState(ResearchJob.State.RUNNING);
        OptimizationEngine engine = new OptimizationEngine(config);
        List<StrategyInput> inputs = strategy.declaredInputs();
        WalkForwardConfig wfCfg = wf != null ? wf : WalkForwardConfig.ofMonths(4, 1, 1);

        // ── Phase 1: grid × timeframes ───────────────────────────────────
        OptimizationRequest.OptimizationRequestBuilder rb = OptimizationRequest.builder()
                .strategy(strategy).timeframes(tfs)
                .barsLoader((id, tf) -> bars.get(id) != null ? bars.get(id).get(tf) : null)
                .parameters(inputs).constraints(constraints);
        scripIds.forEach(rb::scrip);
        engine.optimize(rb.build(), context);

        // ── Phase 2: robust selection per timeframe ──────────────────────
        RobustParameterSelector selector = new RobustParameterSelector(config);
        Map<Timeframe, SelectionResult> selections = new EnumMap<>(Timeframe.class);
        for (Timeframe tf : tfs) {
            try {
                SelectionResult sel = selector.select(context.resultsFor(tf), inputs);
                selections.put(tf, sel);
                context.recordSelection(tf, sel);
                context.warnAll(sel.warnings());
            } catch (IllegalStateException e) {
                log.warn("[{}] selection failed: {}", tf.getCode(), e.getMessage());
                context.warn(ResearchWarning.INSUFFICIENT_DATA);
            }
        }
        if (selections.isEmpty()) {
            context.getJob().setState(ResearchJob.State.COMPLETED);
            return context;
        }

        // ── Phase 3: walk-forward per timeframe ──────────────────────────
        WalkForwardEngine wfEngine = new WalkForwardEngine(config);
        for (Map.Entry<Timeframe, SelectionResult> e : selections.entrySet()) {
            Map<String, BarsArrays> slice = new java.util.LinkedHashMap<>();
            for (String id : scripIds) {
                slice.put(id, bars.get(id).get(e.getKey()));
            }
            try {
                WalkForwardResult r = wfEngine.run(strategy, slice, e.getKey(),
                        inputs, constraints, wfCfg, null, context);
                context.recordWalkForward(e.getKey(), r);
                context.warnAll(r.warnings());
            } catch (IllegalArgumentException ex) {
                log.warn("[{}] walk-forward skipped: {}", e.getKey().getCode(),
                        ex.getMessage());
                context.warn(ResearchWarning.INSUFFICIENT_DATA);
            }
        }

        // ── Phase 6 preliminary: choose anchor timeframe for 4/5/7/8 ─────
        var tfSelection = TimeframeSelector.select(context, config);
        context.setTimeframeSelection(tfSelection);
        Timeframe bestTf = tfSelection.selected();
        if (bestTf == null || !selections.containsKey(bestTf)) {
            context.getJob().setState(ResearchJob.State.COMPLETED);
            return context;
        }
        ParameterCombination combo = selections.get(bestTf).combination();
        List<ScripSlice> slices = new ArrayList<>();
        for (String id : scripIds) {
            slices.add(new ScripSlice(id, bars.get(id).get(bestTf)));
        }

        // ── Phase 4: exit optimization ───────────────────────────────────
        ExitOptimizer exits = new ExitOptimizer(config, engine);
        ExitOptimizationResult exitResult = exits.optimize(strategy, combo,
                slices, bestTf, ExitGrid.builder().build());
        context.recordExitResult(bestTf, exitResult);
        StandardExitPolicy policy = exitResult.policy();

        // ── Phases 5+7 need a detailed run with observations ─────────────
        double riskAtr = policy != null && policy.initialStopAtr() != null
                ? policy.initialStopAtr() : 0;
        TradeRun run = engine.evaluateDetailed(strategy, combo, slices, policy,
                false, true, riskAtr);
        context.setFinalTradeRun(run);
        List<TradeObservation> obs = run.observations().stream()
                .sorted(Comparator.comparingLong(TradeObservation::exitTimestamp))
                .toList();

        // ── Phase 5: filter analysis (≥50 bins, per-bin Sortino of the
        //    filtered trade subset) ───────────────────────────────────────
        FilterAnalyzer fa = new FilterAnalyzer(50, 5, 0.0);
        List<FilterAnalysis> filters = fa.analyze(obs, FilterAnalyzer.DEFAULT_FEATURES);
        context.addFilterAnalyses(filters);

        // ── Phase 7: exploratory analysis ────────────────────────────────
        context.addCalendarAnalysis("Time of day (30m buckets)",
                CalendarAnalyzer.timeOfDay(obs, 30));
        context.addCalendarAnalysis("Day of week", CalendarAnalyzer.dayOfWeek(obs));
        context.addCalendarAnalysis("Week of month", CalendarAnalyzer.weekOfMonth(obs));
        context.addCalendarAnalysis("Month of year", CalendarAnalyzer.monthOfYear(obs));
        context.setStreakResult(StreakAnalyzer.analyze(obs, 5));
        context.setAutocorrelationResult(AutocorrelationAnalyzer.analyze(obs, 10));
        context.setRMultipleResult(RMultipleAnalyzer.analyze(obs, 15));
        context.setDrawdownResult(DrawdownAnalyzer.analyze(run.equityCurve()));

        // ── Phase 8: validation + final configuration ────────────────────
        var mc = MonteCarloAnalyzer.analyze(MonteCarloAnalyzer.pnlOf(run.trades()),
                config.getInitialCapital(), 500, 42L);
        context.setMonteCarloResult(mc);
        var cs = CostSensitivityAnalyzer.analyze(strategy, combo, slices, policy,
                config, new double[]{0.5, 1.0, 1.5, 2.0});
        context.setCostSensitivityResult(cs);

        int hypotheses = context.getOptimizationResults().size() + filters.size() * 10;
        context.audit("hypothesesTested", String.valueOf(hypotheses));
        if (hypotheses > config.getMultipleTestingThreshold()) {
            context.warn(ResearchWarning.POSSIBLE_OVERFITTING);
        }
        if (cs.fragile()) {
            context.warn(ResearchWarning.HIGH_TRANSACTION_COST_SENSITIVITY);
        }
        if (mc.p99MaxDrawdown() > config.getMaxDrawdownThresholdPercent()) {
            context.warn(ResearchWarning.HIGH_DRAWDOWN);
        }
        context.setFinalConfiguration(new FinalConfiguration(strategy.strategyId(),
                scripIds, "LONG", bestTf, combo, policy,
                config.getInitialCapital(), config.getCostPercent(),
                config.getSlippagePercent(), run.metrics(), context.getWarnings()));

        context.getJob().setState(ResearchJob.State.COMPLETED);
        return context;
    }

}
