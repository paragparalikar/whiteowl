package com.whiteowl.core.backtest.rotational.optimizer;

import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.rotational.RotationalBacktestEngine;
import com.whiteowl.core.backtest.rotational.RotationalBacktestResult;
import com.whiteowl.core.backtest.rotational.RotationalMetrics;
import com.whiteowl.core.bar.model.Bars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parameter optimization engine for the rotational ORB backtest.
 *
 * <p>Generates a grid of {@link RotationalBacktestConfig} instances by varying
 * one or two {@link OptimizableParameter}s, runs each through
 * {@link RotationalBacktestEngine} in parallel, and collects results.</p>
 *
 * <p>Thread safety: the engine is safe to run from a background thread.
 * Call {@link #cancel()} to request early termination.</p>
 */
public final class ParameterOptimizationEngine {

    private static final Logger log = LoggerFactory.getLogger(ParameterOptimizationEngine.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Progress callback, invoked after each config completes.
     */
    @FunctionalInterface
    public interface ProgressListener {
        /**
         * @param completed number of configs completed so far
         * @param total     total number of configs
         * @param latest    the result of the latest completed run
         */
        void onProgress(int completed, int total, OptimizationResult latest);
    }

    /**
     * Run the optimization.
     *
     * @param baseConfig    the base config (all non-optimized fields come from here)
     * @param params        1 or 2 parameters to optimize (max 2)
     * @param intradayBars  pre-loaded intraday bars
     * @param dailyBars     pre-loaded daily bars
     * @param listener      progress callback (may be null)
     * @return all results, ordered by grid position
     */
    public List<OptimizationResult> optimize(
            RotationalBacktestConfig baseConfig,
            List<OptimizableParameter> params,
            Map<String, Bars> intradayBars,
            Map<String, Bars> dailyBars,
            ProgressListener listener) {

        if (params.isEmpty() || params.size() > 2) {
            throw new IllegalArgumentException("Must optimize exactly 1 or 2 parameters, got: " + params.size());
        }

        cancelled.set(false);

        // Generate the grid of configs
        List<ConfigPoint> grid = generateGrid(baseConfig, params);
        int total = grid.size();
        log.info("Optimization grid: {} configs ({} parameters)", total,
                params.size() == 1 ? params.get(0).name() : params.get(0).name() + " x " + params.get(1).name());

        // Thread-safe results collection
        List<OptimizationResult> results = Collections.synchronizedList(new ArrayList<>(total));
        AtomicInteger completedCount = new AtomicInteger(0);

        // Run in parallel using ForkJoinPool
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            pool.submit(() -> grid.parallelStream().forEach(point -> {
                if (cancelled.get()) return;

                RotationalMetrics metrics = null;
                try {
                    RotationalBacktestEngine engine = new RotationalBacktestEngine(point.config());
                    RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
                    metrics = result.getMetrics();
                } catch (Exception e) {
                    log.warn("Optimization run failed for params {}: {}", point.paramValues(), e.getMessage());
                }

                OptimizationResult optResult = new OptimizationResult(point.config(), metrics, point.paramValues());
                results.add(optResult);

                int done = completedCount.incrementAndGet();
                if (listener != null) {
                    listener.onProgress(done, total, optResult);
                }
            })).get();
        } catch (Exception e) {
            if (!cancelled.get()) {
                log.error("Optimization failed", e);
            }
        } finally {
            pool.shutdown();
        }

        // Sort results by grid order (param1 value, then param2 value)
        results.sort((a, b) -> {
            int cmp = Double.compare(a.paramValues()[0], b.paramValues()[0]);
            if (cmp != 0 || a.paramValues().length < 2) return cmp;
            return Double.compare(a.paramValues()[1], b.paramValues()[1]);
        });

        return results;
    }

    /** Request cancellation of a running optimization. */
    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    /** Total number of configs that would be generated. */
    public static int gridSize(List<OptimizableParameter> params) {
        int size = 1;
        for (OptimizableParameter p : params) size *= p.gridSize();
        return size;
    }

    // ── Grid generation ──────────────────────────────────────────────

    private record ConfigPoint(RotationalBacktestConfig config, double[] paramValues) {}

    private List<ConfigPoint> generateGrid(RotationalBacktestConfig baseConfig,
                                           List<OptimizableParameter> params) {
        List<ConfigPoint> grid = new ArrayList<>();
        OptimizableParameter p1 = params.get(0);
        OptimizableParameter p2 = params.size() > 1 ? params.get(1) : null;

        for (int i = 0; i < p1.gridSize(); i++) {
            double v1 = p1.valueAt(i);
            if (p2 == null) {
                RotationalBacktestConfig config = applyParam(baseConfig, p1.fieldName(), v1);
                grid.add(new ConfigPoint(config, new double[]{v1}));
            } else {
                for (int j = 0; j < p2.gridSize(); j++) {
                    double v2 = p2.valueAt(j);
                    RotationalBacktestConfig config = applyParam(baseConfig, p1.fieldName(), v1);
                    config = applyParam(config, p2.fieldName(), v2);
                    grid.add(new ConfigPoint(config, new double[]{v1, v2}));
                }
            }
        }

        return grid;
    }

    /**
     * Create a new config by copying the base and overriding one field.
     * Uses the builder's toBuilder()-like approach via reflection on the builder.
     */
    private static RotationalBacktestConfig applyParam(RotationalBacktestConfig base,
                                                        String fieldName, double value) {
        RotationalBacktestConfig.RotationalBacktestConfigBuilder builder = copyToBuilder(base);
        try {
            // Try double first, then int, then Double (nullable)
            Method method = findBuilderMethod(builder, fieldName, value);
            method.invoke(builder, castValue(method.getParameterTypes()[0], value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot set field '" + fieldName + "' on config builder", e);
        }
        return builder.build();
    }

    private static RotationalBacktestConfig.RotationalBacktestConfigBuilder copyToBuilder(
            RotationalBacktestConfig src) {
        return RotationalBacktestConfig.builder()
                .universeGroupName(src.getUniverseGroupName())
                .startDate(src.getStartDate())
                .endDate(src.getEndDate())
                .openingRangeMinutes(src.getOpeningRangeMinutes())
                .barMinutes(src.getBarMinutes())
                .side(src.getSide())
                .minGapAtr(src.getMinGapAtr())
                .maxGapAtr(src.getMaxGapAtr())
                .minOrAtr(src.getMinOrAtr())
                .maxOrAtr(src.getMaxOrAtr())
                .minOrbRvol(src.getMinOrbRvol())
                .maxOrbRvol(src.getMaxOrbRvol())
                .minRsRank(src.getMinRsRank())
                .maxRsRank(src.getMaxRsRank())
                .minOrbIbs(src.getMinOrbIbs())
                .maxOrbIbs(src.getMaxOrbIbs())
                .minOrBodyPct(src.getMinOrBodyPct())
                .maxOrBodyPct(src.getMaxOrBodyPct())
                .minOrSpreadPct(src.getMinOrSpreadPct())
                .maxOrSpreadPct(src.getMaxOrSpreadPct())
                .gapDirectionMode(src.getGapDirectionMode())
                .entryMethod(src.getEntryMethod())
                .maxReEntries(src.getMaxReEntries())
                .picks(src.getPicks())
                .rankerType(src.getRankerType())
                .entryCutoffTime(src.getEntryCutoffTime())
                .exitTime(src.getExitTime())
                .stopBasis(src.getStopBasis())
                .stopMultiplier(src.getStopMultiplier())
                .trailingStopEnabled(src.isTrailingStopEnabled())
                .trailingStopBasis(src.getTrailingStopBasis())
                .trailingStopMultiplier(src.getTrailingStopMultiplier())
                .targetEnabled(src.isTargetEnabled())
                .targetBasis(src.getTargetBasis())
                .targetMultiplier(src.getTargetMultiplier())
                .initialCapital(src.getInitialCapital())
                .slippage(src.getSlippage())
                .atrScaling(src.isAtrScaling());
    }

    private static Method findBuilderMethod(
            RotationalBacktestConfig.RotationalBacktestConfigBuilder builder,
            String fieldName, double value) throws NoSuchMethodException {
        Class<?> builderClass = builder.getClass();
        // Try exact primitive types in order
        for (Class<?> type : new Class<?>[]{double.class, int.class, Double.class, boolean.class}) {
            try {
                return builderClass.getMethod(fieldName, type);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException("No builder method: " + fieldName);
    }

    private static Object castValue(Class<?> type, double value) {
        if (type == int.class || type == Integer.class) return (int) Math.round(value);
        if (type == boolean.class || type == Boolean.class) return value != 0;
        if (type == Double.class) return value;
        return value;
    }
}
