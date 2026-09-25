package com.whiteowl.core.backtest.v2.optimization.exit;

import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.backtest.v2.optimization.TradeRun;
import com.whiteowl.core.backtest.v2.optimization.plateau.RobustParameterSelector;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phase 4 — exit optimization driven by MAE/MFE excursion analysis.
 *
 * <p>For one entry configuration this:</p>
 * <ol>
 *   <li>Generates the raw trade population with only a long time stop and
 *       collects per-trade MAE/MFE (ATR-normalized).</li>
 *   <li>Derives candidate initial stops from the MAE distribution, targets
 *       from the MFE distribution, and time stops from time-to-MFE (explicit
 *       candidate arrays in {@link ExitGrid} override the derivation).</li>
 *   <li>Backtests every stop × target × time-stop combination and selects a
 *       <b>robust region</b> via the Phase-2 plateau machinery — never plain
 *       argmax.</li>
 * </ol>
 */
@Slf4j
public final class ExitOptimizer {

    public static final String STOP_PARAM = "exit.initialStopAtr";
    public static final String TARGET_PARAM = "exit.targetAtr";
    public static final String TIME_STOP_PARAM = "exit.timeStopBars";

    private final ResearchConfiguration config;
    private final OptimizationEngine engine;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ExitOptimizer(ResearchConfiguration config, OptimizationEngine engine) {
        this.config = config;
        this.engine = engine;
    }

    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Optimize exits for one entry configuration on the given slices
     * (development data only — callers must pass research-period slices).
     */
    public ExitOptimizationResult optimize(StrategySpec strategy,
                                            ParameterCombination entryCombination,
                                            List<ScripSlice> slices,
                                            Timeframe timeframe,
                                            ExitGrid grid) {
        cancelled.set(false);

        // 1. Raw trade generation with only the bounding time stop.
        StandardExitPolicy rawPolicy = StandardExitPolicy.timeStopOnly(grid.getRawTradeTimeStop());
        TradeRun raw = engine.evaluateDetailed(strategy, entryCombination, slices,
                rawPolicy, true, false, 0);
        List<Excursion> excursions = raw.excursions();
        log.info("Raw trade population for {}: {}", entryCombination.id(),
                ExcursionAnalyzer.summarize(excursions));

        // 2. Candidate sets (derived from distributions unless configured).
        double[] stops = grid.getStopAtr() != null ? grid.getStopAtr()
                : ExcursionAnalyzer.candidateStops(excursions, 0.25, 5.0, 0.25);
        double[] targets = grid.getTargetAtr() != null ? grid.getTargetAtr()
                : ExcursionAnalyzer.candidateTargets(excursions, 0.5, 8.0, 0.5);
        int[] timeStops = grid.getTimeStopBars() != null ? grid.getTimeStopBars()
                : ExcursionAnalyzer.candidateTimeStops(excursions, 5,
                        grid.getRawTradeTimeStop(), 5);

        // 3. Build the pseudo parameter space over enabled exit dimensions.
        List<StrategyInput> pseudoInputs = new ArrayList<>();
        List<String> dims = new ArrayList<>();
        List<List<Number>> axes = new ArrayList<>();
        addDim(pseudoInputs, dims, axes, STOP_PARAM,
                grid.isStopEnabled() ? toNumbers(stops) : null);
        addDim(pseudoInputs, dims, axes, TARGET_PARAM,
                grid.isTargetEnabled() ? toNumbers(targets) : null);
        addDim(pseudoInputs, dims, axes, TIME_STOP_PARAM,
                grid.isTimeStopEnabled() ? toNumbers(timeStops) : null);

        List<ParameterCombination> exitCombos = cartesian(dims, axes);
        if (exitCombos.isEmpty()) {
            throw new IllegalStateException("Exit grid is empty — enable at "
                    + "least one exit dimension");
        }
        log.info("Exit candidates for {}: {} (stops={}, targets={}, timeStops={})",
                entryCombination.id(), exitCombos.size(), stops.length,
                targets.length, timeStops.length);

        // 4. Evaluate every exit policy (parallel, failure-isolated).
        OptimizationResult[] evaluated = new OptimizationResult[exitCombos.size()];
        int workers = config.getWorkerCount() <= 0
                ? Runtime.getRuntime().availableProcessors()
                : config.getWorkerCount();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(workers, exitCombos.size()));
        try {
            List<Future<?>> futures = new ArrayList<>(exitCombos.size());
            for (int i = 0; i < exitCombos.size(); i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    if (cancelled.get()) {
                        evaluated[idx] = OptimizationResult.failed(
                                timeframe, exitCombos.get(idx), "cancelled");
                        return;
                    }
                    ParameterCombination exitCombo = exitCombos.get(idx);
                    try {
                        StandardExitPolicy policy = toPolicy(exitCombo, grid.getAtrPeriod());
                        OptimizationMetrics m = engine.evaluateCombination(
                                strategy, entryCombination, slices, policy);
                        evaluated[idx] = OptimizationResult.ok(timeframe, exitCombo, m,
                                config.getMinTradesPerCombination());
                    } catch (Exception e) {
                        evaluated[idx] = OptimizationResult.failed(
                                timeframe, exitCombo, e.getMessage());
                    }
                }));
            }
            for (Future<?> f : futures) f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Exit optimization interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            pool.shutdown();
        }

        // 5. Robust selection over the exit dimensions.
        List<OptimizationResult> results = List.of(evaluated);
        RobustParameterSelector selector = new RobustParameterSelector(config);
        SelectionResult selection = selector.select(results, pseudoInputs);
        StandardExitPolicy policy = toPolicy(selection.combination(), grid.getAtrPeriod());
        OptimizationMetrics metrics = selection.selectedResult().metrics();

        log.info("Exit selected for {}: {}", entryCombination.id(), selection.combination().id());
        return new ExitOptimizationResult(entryCombination, excursions, results,
                selection, policy, metrics);
    }

    // ── Internals ────────────────────────────────────────────────────────

    private void addDim(List<StrategyInput> inputs, List<String> dims,
                         List<List<Number>> axes, String name, List<Number> values) {
        if (values == null || values.isEmpty()) {
            return; // disabled dimension — policy component stays null
        }
        dims.add(name);
        axes.add(values);
        if (values.size() >= config.getMinPlateauPoints()) {
            double min = values.get(0).doubleValue();
            double max = values.get(values.size() - 1).doubleValue();
            inputs.add(new StrategyInput(name, values.get(0), min, max));
        } else {
            // Too few points for plateau analysis — treat as fixed.
            inputs.add(new StrategyInput(name, values.get(0)));
        }
    }

    private static List<Number> toNumbers(double[] values) {
        List<Number> out = new ArrayList<>(values.length);
        for (double v : values) out.add(v);
        return out;
    }

    private static List<Number> toNumbers(int[] values) {
        List<Number> out = new ArrayList<>(values.length);
        for (int v : values) out.add(v);
        return out;
    }

    private static List<ParameterCombination> cartesian(List<String> dims,
                                                         List<List<Number>> axes) {
        List<ParameterCombination> out = new ArrayList<>();
        int[] idx = new int[axes.size()];
        if (axes.isEmpty()) {
            return out;
        }
        while (true) {
            Map<String, Number> map = new LinkedHashMap<>();
            for (int i = 0; i < dims.size(); i++) {
                map.put(dims.get(i), axes.get(i).get(idx[i]));
            }
            out.add(new ParameterCombination(map));
            int pos = axes.size() - 1;
            while (pos >= 0 && ++idx[pos] >= axes.get(pos).size()) {
                idx[pos] = 0;
                pos--;
            }
            if (pos < 0) break;
        }
        return out;
    }

    static StandardExitPolicy toPolicy(ParameterCombination exitCombo, int atrPeriod) {
        Double stop = numberOrNull(exitCombo.get(STOP_PARAM));
        Double target = numberOrNull(exitCombo.get(TARGET_PARAM));
        Integer timeStop = exitCombo.get(TIME_STOP_PARAM) != null
                ? exitCombo.get(TIME_STOP_PARAM).intValue() : null;
        return new StandardExitPolicy(stop, target, null, timeStop, atrPeriod);
    }

    private static Double numberOrNull(Number n) {
        return n == null ? null : n.doubleValue();
    }

}
