package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.feature.CompositeTradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.optimization.analysis.EntryFeatureCollector;
import com.whiteowl.core.backtest.v2.optimization.analysis.TradeObservation;
import com.whiteowl.core.backtest.v2.optimization.exit.Excursion;
import com.whiteowl.core.backtest.v2.optimization.exit.ExcursionCollector;
import com.whiteowl.core.backtest.v2.model.EquityCurve;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 1 — core optimization engine.
 *
 * <p>Evaluates every (timeframe × parameter combination) on the requested
 * instruments and returns a numbers-only {@link OptimizationMetrics} per pair.
 * Bar data is loaded once per (scrip, timeframe) and shared read-only across
 * combinations; strategy instances are created fresh per run.</p>
 *
 * <p>Execution is bounded-parallel, deterministic in result order, and
 * failure-isolated: one broken combination produces a
 * {@link OptimizationResult.Status#FAILED} record, never an aborted run.</p>
 */
@Slf4j
public final class OptimizationEngine {

    private final ResearchConfiguration config;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public OptimizationEngine(ResearchConfiguration config) {
        this.config = config;
    }

    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Run the full grid over all requested timeframes and scrips. Results are
     * appended to the context (if non-null) and returned in deterministic
     * (timeframe, grid) order.
     */
    public List<OptimizationResult> optimize(OptimizationRequest request,
                                              OptimizationContext context) {
        cancelled.set(false);
        List<StrategyInput> inputs = request.getParameters() == null
                || request.getParameters().isEmpty()
                ? request.getStrategy().declaredInputs()
                : request.getParameters();
        List<ParameterCombination> grid = ParameterGrid.generate(inputs,
                request.getConstraints() == null ? List.of() : request.getConstraints(),
                config.getMaxParameterCombinations());
        if (grid.isEmpty()) {
            throw new IllegalArgumentException("Parameter grid is empty");
        }

        List<OptimizationResult> all = new ArrayList<>();
        for (Timeframe tf : request.getTimeframes()) {
            if (cancelled.get()) break;
            if (context != null) {
                context.getJob().setCurrentTimeframe(tf.getCode());
                context.getJob().addTotalTasks(grid.size());
            }
            List<ScripSlice> slices = loadSlices(request, tf);
            if (slices.isEmpty()) {
                log.warn("No data for timeframe {} — skipping", tf);
                continue;
            }
            List<OptimizationResult> tfResults = evaluateGrid(
                    request.getStrategy(), grid, slices, tf, request.getListener(), context);
            all.addAll(tfResults);
            if (context != null) {
                context.addOptimizationResults(tfResults);
            }
        }
        return all;
    }

    /**
     * Evaluate a grid of combinations on prepared slices. Shared by the public
     * {@link #optimize} path and by the walk-forward in-sample procedure.
     */
    public List<OptimizationResult> evaluateGrid(StrategySpec strategy,
                                                  List<ParameterCombination> grid,
                                                  List<ScripSlice> slices,
                                                  Timeframe timeframe,
                                                  OptimizationProgressListener listener,
                                                  OptimizationContext context) {
        int total = grid.size();
        OptimizationResult[] results = new OptimizationResult[total];
        int workers = config.getWorkerCount() <= 0
                ? Runtime.getRuntime().availableProcessors()
                : config.getWorkerCount();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(workers, total));
        AtomicInteger done = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>(total);
            for (int i = 0; i < total; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    if (cancelled.get()) {
                        results[idx] = OptimizationResult.failed(
                                timeframe, grid.get(idx), "cancelled");
                        return;
                    }
                    ParameterCombination combo = grid.get(idx);
                    try {
                        OptimizationMetrics m = evaluateCombination(strategy, combo, slices);
                        results[idx] = OptimizationResult.ok(
                                timeframe, combo, m, config.getMinTradesPerCombination());
                    } catch (Exception e) {
                        log.warn("Combination {} failed: {}", combo.id(), e.getMessage());
                        results[idx] = OptimizationResult.failed(
                                timeframe, combo, e.getMessage());
                    }
                    int c = done.incrementAndGet();
                    OptimizationResult r = results[idx];
                    if (context != null) {
                        if (r.status() == OptimizationResult.Status.FAILED) {
                            context.getJob().taskFailed();
                        }
                        context.getJob().taskCompleted();
                    }
                    if (listener != null) {
                        listener.onProgress(c, total, r);
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Optimization interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Optimization task failed fatally", e.getCause());
        } finally {
            pool.shutdown();
        }
        return List.of(results);
    }

    /**
     * Backtest one combination over all slices and compute aggregate metrics.
     * Warmup bars inside a slice do not contaminate the result: only trades
     * entered inside the slice's eval range count, and the equity curve is
     * trimmed to it.
     */
    public OptimizationMetrics evaluateCombination(StrategySpec strategy,
                                                    ParameterCombination combination,
                                                    List<ScripSlice> slices) {
        return evaluateCombination(strategy, combination, slices, null);
    }

    /** Same, with engine-side standardized exits applied. */
    public OptimizationMetrics evaluateCombination(StrategySpec strategy,
                                                    ParameterCombination combination,
                                                    List<ScripSlice> slices,
                                                    StandardExitPolicy exits) {
        return evaluateDetailed(strategy, combination, slices, exits,
                false, false, 0).metrics();
    }

    /**
     * Like {@link #evaluateCombination} but also returns merged trades, the
     * merged equity curve, and — when collectors are requested — per-trade
     * MAE/MFE excursions and entry-feature observations.
     *
     * @param collectExcursions   attach an ExcursionCollector per scrip run
     * @param collectObservations attach an EntryFeatureCollector per scrip run
     * @param riskAtrMultiple     ATR multiple used as initial risk for
     *                            R-multiple computation (0 → NaN)
     */
    public TradeRun evaluateDetailed(StrategySpec strategy,
                                      ParameterCombination combination,
                                      List<ScripSlice> slices,
                                      StandardExitPolicy exits,
                                      boolean collectExcursions,
                                      boolean collectObservations,
                                      double riskAtrMultiple) {
        List<ScripResult> results = new ArrayList<>(slices.size());
        List<Excursion> excursions = new ArrayList<>();
        List<TradeObservation> observations = new ArrayList<>();
        for (ScripSlice slice : slices) {
            List<TradeLifecycleCallback> callbacks = new ArrayList<>(2);
            ExcursionCollector ec = collectExcursions
                    ? new ExcursionCollector(exits != null ? exits.atrPeriod()
                            : StandardExitPolicy.DEFAULT_ATR_PERIOD)
                    : null;
            EntryFeatureCollector fc = collectObservations
                    ? new EntryFeatureCollector(
                            exits != null ? exits.atrPeriod()
                                    : StandardExitPolicy.DEFAULT_ATR_PERIOD,
                            14, 14, 14, 20, riskAtrMultiple)
                    : null;
            if (ec != null) callbacks.add(ec);
            if (fc != null) callbacks.add(fc);
            TradeLifecycleCallback callback = callbacks.isEmpty() ? null
                    : callbacks.size() == 1 ? callbacks.get(0)
                    : new CompositeTradeLifecycleCallback(callbacks);
            BacktestEngine engine = new BacktestEngine(
                    config.getInitialCapital(), config.getCostPercent(),
                    config.getSlippagePercent(), config.getVolumeParticipationPercent(),
                    callback, exits);
            TradingStrategyBase s = strategy.newInstance();
            s.setInputOverrides(combination.values());
            ScripResult r = engine.run(slice.scripId(), slice.arrays(), s);
            results.add(trimToEvalRange(r, slice));
            if (ec != null) excursions.addAll(ec.excursions());
            if (fc != null) observations.addAll(fc.observations());
        }
        List<TradeRecord> trades = new ArrayList<>();
        for (ScripResult r : results) {
            trades.addAll(r.getTrades());
        }
        trades.sort(Comparator.comparingLong(TradeRecord::getEntryTimestamp));
        EquityCurve curve = results.stream()
                .map(ScripResult::getEquityCurve)
                .filter(c2 -> c2 != null && c2.getSize() > 1)
                .findFirst().orElse(null);
        return new TradeRun(
                OptimizationMetrics.of(results, config.getInitialCapital()),
                trades, curve, excursions, observations);
    }

    // ── Internals ────────────────────────────────────────────────────────

    private List<ScripSlice> loadSlices(OptimizationRequest request, Timeframe tf) {
        List<ScripSlice> slices = new ArrayList<>();
        for (String scripId : request.getScripIds()) {
            try {
                BarsArrays arrays = request.getBarsLoader().load(scripId, tf);
                if (arrays == null || arrays.size() == 0) {
                    log.warn("No bars for {} @ {}", scripId, tf);
                    continue;
                }
                if (request.getDataSplit() != null) {
                    DataSplit split = request.getDataSplit();
                    arrays = BarsSlicer.subrange(arrays, split.startIndex(), split.endIndex());
                }
                if (arrays.size() > 0) {
                    slices.add(new ScripSlice(scripId, arrays));
                }
            } catch (Exception e) {
                log.error("Failed loading bars for {} @ {}: {}", scripId, tf, e.getMessage());
            }
        }
        return slices;
    }

    /**
     * Filter trades to entries inside the slice eval range and trim the equity
     * curve to the same range.
     */
    private static ScripResult trimToEvalRange(ScripResult r, ScripSlice slice) {
        if (slice.evalStartIndex() == 0 && slice.evalEndIndex() == slice.arrays().size()) {
            return r;
        }
        List<TradeRecord> trades = new ArrayList<>();
        float pnl = 0f;
        for (TradeRecord t : r.getTrades()) {
            if (t.getEntryBarIndex() >= slice.evalStartIndex()
                    && t.getEntryBarIndex() < slice.evalEndIndex()) {
                trades.add(t);
                pnl += t.getNetPnl();
            }
        }
        EquityCurve curve = r.getEquityCurve();
        if (curve != null && curve.getSize() == slice.arrays().size()) {
            int from = slice.evalStartIndex();
            int to = Math.min(slice.evalEndIndex(), curve.getSize());
            float[] values = new float[to - from];
            long[] ts = new long[to - from];
            System.arraycopy(curve.getValues(), from, values, 0, to - from);
            System.arraycopy(curve.getTimestamps(), from, ts, 0, to - from);
            curve = new EquityCurve(values, ts, to - from);
        }
        return ScripResult.builder()
                .scripId(r.getScripId())
                .trades(trades)
                .equityCurve(curve)
                .totalNetPnl(pnl)
                .build();
    }

}
