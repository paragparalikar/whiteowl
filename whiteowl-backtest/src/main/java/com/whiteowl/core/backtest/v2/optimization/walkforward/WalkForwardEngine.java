package com.whiteowl.core.backtest.v2.optimization.walkforward;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.BarsSlicer;
import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ParameterConstraint;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3 — generic walk-forward validation engine.
 *
 * <p>Replaces the retired rotational-only {@code WalkForwardOptimizationDriver}.
 * This engine is strategy-agnostic: each window delegates in-sample selection
 * to an {@link OptimizationProcedure} (grid + plateau selection by default),
 * then freezes the chosen combination and evaluates it on the next,
 * never-before-seen window.</p>
 *
 * <p>Slices carry warmup bars so indicators are warm at the window boundary;
 * only trades entered inside the evaluation range count toward metrics.</p>
 */
@Slf4j
public final class WalkForwardEngine {

    private final ResearchConfiguration config;
    private final OptimizationEngine engine;

    public WalkForwardEngine(ResearchConfiguration config) {
        this.config = config;
        this.engine = new OptimizationEngine(config);
    }

    /**
     * Run walk-forward over full-history bars for each scrip.
     *
     * @param strategy    the strategy under research
     * @param scripBars   scripId → full-history bars (already loaded once)
     * @param timeframe   the timeframe being validated
     * @param inputs      optimizable parameters (used by the default procedure
     *                    and for drift statistics)
     * @param constraints parameter validity constraints
     * @param windows     window geometry
     * @param procedure   custom in-sample procedure; null →
     *                    {@link GridOptimizationProcedure}
     * @param context     optional shared context — windows are recorded into it
     */
    public WalkForwardResult run(StrategySpec strategy,
                                  Map<String, BarsArrays> scripBars,
                                  Timeframe timeframe,
                                  List<StrategyInput> inputs,
                                  List<ParameterConstraint> constraints,
                                  WalkForwardConfig windows,
                                  OptimizationProcedure procedure,
                                  OptimizationContext context) {
        OptimizationProcedure proc = procedure != null ? procedure
                : new GridOptimizationProcedure(strategy, inputs, constraints,
                        timeframe, config, engine);

        long tMin = Long.MAX_VALUE;
        long tMax = Long.MIN_VALUE;
        for (BarsArrays a : scripBars.values()) {
            if (a.size() > 0) {
                tMin = Math.min(tMin, a.timestamp()[0]);
                tMax = Math.max(tMax, a.timestamp()[a.size() - 1]);
            }
        }
        if (tMin > tMax) {
            throw new IllegalArgumentException("No bar data supplied");
        }

        // Enumerate windows on the global timeline.
        List<long[]> ranges = new ArrayList<>();
        long trainStart = tMin;
        long trainEnd = tMin + windows.getTrainMillis();
        while (trainEnd + windows.getTestMillis() <= tMax) {
            ranges.add(new long[]{trainStart, trainEnd, trainEnd + windows.getTestMillis()});
            if (!windows.isAnchored()) {
                trainStart += windows.getStepMillis();
            }
            trainEnd += windows.getStepMillis();
        }
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException(
                    "Data range too short for the configured walk-forward windows");
        }
        if (context != null) {
            context.getJob().setCurrentPhase("WALK_FORWARD");
            context.getJob().setCurrentTimeframe(timeframe.getCode());
            context.getJob().addTotalTasks(ranges.size());
        }

        List<WalkForwardWindowResult> results = new ArrayList<>(ranges.size());
        int wi = 0;
        for (long[] range : ranges) {
            try {
                List<ScripSlice> inSample = new ArrayList<>();
                List<ScripSlice> outOfSample = new ArrayList<>();
                for (Map.Entry<String, BarsArrays> e : scripBars.entrySet()) {
                    ScripSlice is = slice(e.getKey(), e.getValue(),
                            range[0], range[1], windows.getWarmupBars());
                    ScripSlice oos = slice(e.getKey(), e.getValue(),
                            range[1], range[2], windows.getWarmupBars());
                    if (is != null) inSample.add(is);
                    if (oos != null) outOfSample.add(oos);
                }
                if (inSample.isEmpty() || outOfSample.isEmpty()) {
                    results.add(new WalkForwardWindowResult(wi, range[0], range[1],
                            range[2], null, null, null, "no data in window"));
                    continue;
                }
                ProcedureResult pr = proc.optimize(inSample);
                OptimizationMetrics oos = engine.evaluateCombination(
                        strategy, pr.combination(), outOfSample);
                results.add(new WalkForwardWindowResult(wi, range[0], range[1], range[2],
                        pr.combination(), pr.inSampleMetrics(), oos, null));
            } catch (Exception e) {
                log.warn("Walk-forward window {} failed: {}", wi, e.getMessage());
                results.add(new WalkForwardWindowResult(wi, range[0], range[1], range[2],
                        null, null, null, e.getMessage()));
            } finally {
                wi++;
                if (context != null) {
                    context.getJob().taskCompleted();
                }
            }
        }

        WalkForwardResult result = aggregate(results, inputs);
        if (context != null) {
            context.recordWalkForward(timeframe, result);
            context.warnAll(result.warnings());
        }
        return result;
    }

    /**
     * Slice bars to {@code [startTs, endTs)} preceded by up to
     * {@code warmupBars} bars of warmup. Returns null when the window has no
     * evaluable bars for this scrip.
     */
    private static ScripSlice slice(String scripId, BarsArrays arrays,
                                     long startTs, long endTs, int warmupBars) {
        int evalStart = BarsSlicer.firstIndexAtOrAfter(arrays, startTs);
        int evalEnd = BarsSlicer.firstIndexAtOrAfter(arrays, endTs);
        if (evalEnd <= evalStart) {
            return null;
        }
        int sliceStart = Math.max(0, evalStart - warmupBars);
        BarsArrays sub = BarsSlicer.subrange(arrays, sliceStart, evalEnd);
        return new ScripSlice(scripId, sub, evalStart - sliceStart, sub.size());
    }

    private WalkForwardResult aggregate(List<WalkForwardWindowResult> windows,
                                         List<StrategyInput> inputs) {
        List<WalkForwardWindowResult> ok = windows.stream()
                .filter(w -> !w.failed())
                .toList();

        Map<String, ParameterDriftStats> drift = new LinkedHashMap<>();
        for (StrategyInput input : inputs) {
            List<Double> values = new ArrayList<>(ok.size());
            for (WalkForwardWindowResult w : ok) {
                Number v = w.combination() == null ? null
                        : w.combination().get(input.getName());
                if (v != null) values.add(v.doubleValue());
            }
            if (!values.isEmpty()) {
                drift.put(input.getName(), ParameterDriftStats.of(
                        input.getName(), values, config.getParameterDriftCvThreshold()));
            }
        }

        double isSum = 0, oosSum = 0;
        int isW = 0, oosW = 0, oosTrades = 0;
        for (WalkForwardWindowResult w : ok) {
            if (w.inSample() != null) {
                int t = Math.max(1, w.inSample().totalTrades());
                isSum += w.inSample().sortinoRatio() * t;
                isW += t;
            }
            if (w.outOfSample() != null) {
                int t = Math.max(1, w.outOfSample().totalTrades());
                oosSum += w.outOfSample().sortinoRatio() * t;
                oosW += t;
                oosTrades += w.outOfSample().totalTrades();
            }
        }
        double meanIs = isW > 0 ? isSum / isW : 0;
        double meanOos = oosW > 0 ? oosSum / oosW : 0;
        double wfe = Math.abs(meanIs) > 1e-9 ? meanOos / meanIs : 0;

        List<ResearchWarning> warnings = new ArrayList<>();
        for (ParameterDriftStats d : drift.values()) {
            if (!d.stable()) {
                warnings.add(ResearchWarning.HIGH_WALK_FORWARD_PARAMETER_DRIFT);
                break;
            }
        }
        if (meanOos < config.getOosDegradationThreshold() * meanIs) {
            warnings.add(ResearchWarning.POOR_OUT_OF_SAMPLE_PERFORMANCE);
        }
        if (oosTrades < config.getMinTradesForStrategy()) {
            warnings.add(ResearchWarning.LOW_TRADE_COUNT);
        }
        if (ok.size() < windows.size()) {
            warnings.add(ResearchWarning.INSUFFICIENT_DATA);
        }

        return new WalkForwardResult(List.copyOf(windows), drift,
                meanIs, meanOos, wfe, oosTrades, warnings);
    }

}
