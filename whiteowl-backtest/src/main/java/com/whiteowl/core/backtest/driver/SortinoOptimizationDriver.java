package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.optimizer.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Full parameter optimization for the Rotational ORB strategy, optimizing for
 * SORTINO ratio with robustness-first parameter selection.
 *
 * <p>Key design principles:</p>
 * <ul>
 *   <li><b>Primary metric:</b> Sortino ratio (penalizes downside deviation only)</li>
 *   <li><b>Robust selection:</b> For each parameter, the chosen value must have
 *       good Sortino on BOTH neighboring values (plateau &gt; peak). A parameter
 *       at a sharp peak is fragile — a small data change could flip performance.</li>
 *   <li><b>Stability score:</b> For each grid point, we compute:
 *       {@code stabilityScore = 0.5 * sortino[i] + 0.25 * sortino[i-1] + 0.25 * sortino[i+1]}.
 *       The parameter with the highest stability score wins.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.SortinoOptimizationDriver
 * </pre>
 */
public final class SortinoOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(SortinoOptimizationDriver.class);

    // ── Scoring ──────────────────────────────────────────────────────────

    /** Primary score: Sortino ratio with trade-count and drawdown guards. */
    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 100) return Double.NEGATIVE_INFINITY;
        double s = m.getSortino();
        // Drawdown penalties to guard against strategies that blow up
        if (m.getMaxDrawdown() < -0.15) s -= 2.0;
        else if (m.getMaxDrawdown() < -0.10) s -= 1.0;
        return s;
    }

    /**
     * Stability score for a 1D grid: weighted average of the point and its neighbors.
     * Points at the edges (no neighbor on one side) are penalized by using 0 for
     * the missing neighbor, making edge picks inherently less favorable.
     */
    private static double stabilityScore(double[] scores, int idx) {
        double self = scores[idx];
        double left = (idx > 0) ? scores[idx - 1] : 0;
        double right = (idx < scores.length - 1) ? scores[idx + 1] : 0;
        return 0.50 * self + 0.25 * left + 0.25 * right;
    }

    public static void main(String[] args) throws Exception {
        log.info("==========================================================================================");
        log.info("  Full Parameter Optimization — Sortino-Optimized with Robustness Selection");
        log.info("==========================================================================================");
        log.info("");

        // ── Load data ────────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());
        log.info("");

        int cores = Runtime.getRuntime().availableProcessors();
        log.info("Available cores: {} (using {} for parallel runs)", cores, Math.max(1, cores - 1));
        log.info("");

        // ── Starting config (current production) ─────────────────────────
        RotationalBacktestConfig best = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.0)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.0)
                .trailingStopEnabled(false)
                .entryCutoffTime(LocalTime.of(14, 0))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(0.001)
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();

        // Run baseline
        RotationalMetrics baseline = runSingle(best, intradayBars, dailyBars);
        log.info("BASELINE: Sortino={} Sharpe={} PnL={} MaxDD={}% Trades={} WR={}%",
                fmt(baseline.getSortino()), fmt(baseline.getSharpe()),
                fmtPnl(baseline.getLongPnl() + baseline.getShortPnl()),
                fmt(baseline.getMaxDrawdown() * 100), baseline.getTotalTrades(),
                fmt(baseline.getWinRate() * 100));
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 1: Stop Multiplier x Target Multiplier
        // ══════════════════════════════════════════════════════════════════
        best = optimize2D(best, intradayBars, dailyBars,
                "Phase 1: Stop x Target",
                new OptimizableParameter("StopMult", "stopMultiplier", 0.5, 2.0, 0.25),
                new OptimizableParameter("TargetMult", "targetMultiplier", 1.0, 5.0, 0.5));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 2: Entry Cutoff Time
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig cutoffBase = best;
        LocalTime[] cutoffTimes = {
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(12, 30),
                LocalTime.of(13, 0), LocalTime.of(13, 30),
                LocalTime.of(14, 0), LocalTime.of(14, 30),
                LocalTime.of(15, 0), LocalTime.of(15, 15)
        };
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 2: Entry Cutoff", "EntryCutoff",
                cutoffTimes.length,
                i -> cutoffTimes[i] != null ? cutoffTimes[i].toString() : "None",
                i -> {
                    var b = copyToBuilder(cutoffBase);
                    b.entryCutoffTime(cutoffTimes[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 3: Exit Time
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig exitBase = best;
        LocalTime[] exitTimes = {
                null,
                LocalTime.of(14, 30), LocalTime.of(14, 45),
                LocalTime.of(15, 0), LocalTime.of(15, 10),
                LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)
        };
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 3: Exit Time", "ExitTime",
                exitTimes.length,
                i -> exitTimes[i] != null ? exitTimes[i].toString() : "MktClose",
                i -> {
                    var b = copyToBuilder(exitBase);
                    b.exitTime(exitTimes[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 4: Trailing Stop
        // ══════════════════════════════════════════════════════════════════
        best = optimizeTrailingStop(best, intradayBars, dailyBars);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 5: Min Gap ATR Filter
        // ══════════════════════════════════════════════════════════════════
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 5: Min Gap ATR",
                new OptimizableParameter("MinGapATR", "minGapAtr", 0.0, 1.5, 0.10));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 6: Long Picks x Short Picks
        // ══════════════════════════════════════════════════════════════════
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 6: Picks",
                new OptimizableParameter("Picks", "picks", 1, 10, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 8: Max Re-Entries
        // ══════════════════════════════════════════════════════════════════
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 8: Max Re-Entries",
                new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 9: OR IBS Filter
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig ibsBase = best;
        Double[] ibsValues = {null, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 9: Max OR IBS", "MaxOrbIBS",
                ibsValues.length,
                i -> ibsValues[i] != null ? fmt(ibsValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(ibsBase);
                    b.maxOrbIbs(ibsValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 10: ATR Scaling
        // ══════════════════════════════════════════════════════════════════
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 10: ATR Scaling",
                new OptimizableParameter("AtrScaling", "atrScaling", 0, 1, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 11: Refinement — fine-tune stop x target
        // ══════════════════════════════════════════════════════════════════
        double bs = best.getStopMultiplier();
        double bt = best.getTargetMultiplier();
        best = optimize2D(best, intradayBars, dailyBars,
                "Phase 11: Refine Stop x Target",
                new OptimizableParameter("StopMult", "stopMultiplier",
                        Math.max(0.25, bs - 0.30), bs + 0.30, 0.05),
                new OptimizableParameter("TargetMult", "targetMultiplier",
                        Math.max(0.5, bt - 1.0), bt + 1.0, 0.25));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 12: Refinement — fine-tune gap filter
        // ══════════════════════════════════════════════════════════════════
        double bg = best.getMinGapAtr() != null ? best.getMinGapAtr() : 0.0;
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 12: Refine Gap ATR",
                new OptimizableParameter("MinGapATR", "minGapAtr",
                        Math.max(0.0, bg - 0.20), bg + 0.20, 0.05));

        // ══════════════════════════════════════════════════════════════════
        //  FINAL RESULT
        // ══════════════════════════════════════════════════════════════════
        log.info("");
        log.info("##########################################################################################");
        log.info("  OPTIMIZATION COMPLETE — FINAL OPTIMAL CONFIG (Sortino-Optimized, Robustness-Selected)");
        log.info("##########################################################################################");
        log.info("");

        RotationalMetrics finalMetrics = runSingle(best, intradayBars, dailyBars);

        log.info("  FINAL METRICS:");
        log.info("  Sortino:       {}", fmt(finalMetrics.getSortino()));
        log.info("  Sharpe:        {}", fmt(finalMetrics.getSharpe()));
        log.info("  CAGR:          {}%", fmt(finalMetrics.getCagr() * 100));
        log.info("  Max Drawdown:  {}%", fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("  Profit Factor: {}", fmt(finalMetrics.getProfitFactor()));
        log.info("  Win Rate:      {}%", fmt(finalMetrics.getWinRate() * 100));
        log.info("  Total Trades:  {}", finalMetrics.getTotalTrades());
        log.info("  Net PnL:       {}", fmtPnl(finalMetrics.getLongPnl() + finalMetrics.getShortPnl()));
        log.info("  Long PnL:      {}", fmtPnl(finalMetrics.getLongPnl()));
        log.info("  Short PnL:     {}", fmtPnl(finalMetrics.getShortPnl()));
        log.info("");

        log.info("  FINAL CONFIG:");
        log.info("  Stop:          {} x {}", best.getStopBasis(), fmt(best.getStopMultiplier()));
        log.info("  Target:        {} x {}", best.getTargetBasis(), fmt(best.getTargetMultiplier()));
        log.info("  TrailingStop:  {} ({}x {})", best.isTrailingStopEnabled(),
                fmt(best.getTrailingStopMultiplier()), best.getTrailingStopBasis());
        log.info("  Entry Cutoff:  {}", best.getEntryCutoffTime());
        log.info("  Exit Time:     {}", best.getExitTime() != null ? best.getExitTime() : "MarketClose");
        log.info("  MinGapATR:     {}", best.getMinGapAtr() != null ? fmt(best.getMinGapAtr()) : "None");
        log.info("  Picks:         {} {}", best.getPicks(), best.getSide());
        log.info("  MaxReEntries:  {}", best.getMaxReEntries());
        log.info("  MaxOrbIbs:     {}", best.getMaxOrbIbs() != null ? fmt(best.getMaxOrbIbs()) : "None");
        log.info("  ATR Scaling:   {}", best.isAtrScaling());
        log.info("");

        log.info("  IMPROVEMENT OVER BASELINE:");
        log.info("  {}", String.format("  Sortino: %s -> %s (%+.2f)", fmt(baseline.getSortino()),
                fmt(finalMetrics.getSortino()), finalMetrics.getSortino() - baseline.getSortino()));
        log.info("  {}", String.format("  Sharpe:  %s -> %s (%+.2f)", fmt(baseline.getSharpe()),
                fmt(finalMetrics.getSharpe()), finalMetrics.getSharpe() - baseline.getSharpe()));
        log.info("  CAGR:    {}% -> {}%", fmt(baseline.getCagr() * 100), fmt(finalMetrics.getCagr() * 100));
        log.info("  MaxDD:   {}% -> {}%", fmt(baseline.getMaxDrawdown() * 100), fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("");
        log.info("##########################################################################################");
    }

    // ── 1D optimization with stability-based selection ────────────────────

    private static RotationalBacktestConfig optimize1D(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            String phaseLabel, OptimizableParameter param) throws Exception {

        log.info("==========================================================================================");
        log.info("  {}", phaseLabel);
        log.info("==========================================================================================");

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = param.gridSize();
        log.info("  Grid: {} points for {}", total, param.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(param),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 5 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        // Compute scores and stability scores
        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score(results.get(i).metrics());
        }

        double[] stabScores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            stabScores[i] = stabilityScore(scores, i);
        }

        // Log table
        log.info("");
        log.info("  {}", String.format("%-12s %8s %8s %8s %10s %7s %8s %8s",
                param.name(), "Sortino", "Sharpe", "MaxDD%", "PnL", "Trades", "Score", "Stable"));
        log.info("  {}", "-".repeat(85));

        int bestIdx = 0;
        double bestStab = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < results.size(); i++) {
            if (stabScores[i] > bestStab) {
                bestStab = stabScores[i];
                bestIdx = i;
            }
        }

        for (int i = 0; i < results.size(); i++) {
            RotationalMetrics m = results.get(i).metrics();
            if (m == null) continue;
            String marker = (i == bestIdx) ? " <-- BEST" : "";
            log.info("  {}", String.format("%-12s %8.2f %8.2f %7.1f%% %10s %7d %8.2f %8.2f%s",
                    fmt(results.get(i).paramValues()[0]),
                    m.getSortino(), m.getSharpe(),
                    m.getMaxDrawdown() * 100,
                    fmtPnl(m.getLongPnl() + m.getShortPnl()),
                    m.getTotalTrades(), scores[i], stabScores[i], marker));
        }

        OptimizationResult chosen = results.get(bestIdx);
        log.info("");
        log.info("  >> Robust best {}: {} (stability={}, raw={})",
                param.name(), fmt(chosen.paramValues()[0]),
                fmt(stabScores[bestIdx]), fmt(scores[bestIdx]));
        logMetrics(chosen.metrics());
        return chosen.config();
    }

    // ── 2D optimization with stability-based selection ────────────────────

    private static RotationalBacktestConfig optimize2D(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            String phaseLabel,
            OptimizableParameter p1, OptimizableParameter p2) throws Exception {

        log.info("==========================================================================================");
        log.info("  {}", phaseLabel);
        log.info("==========================================================================================");

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = p1.gridSize() * p2.gridSize();
        log.info("  Grid: {} x {} = {} points ({} x {})",
                p1.gridSize(), p2.gridSize(), total, p1.name(), p2.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(p1, p2),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 10 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        // Build 2D score grid
        int n1 = p1.gridSize(), n2 = p2.gridSize();
        double[][] scoreGrid = new double[n1][n2];
        RotationalMetrics[][] metricsGrid = new RotationalMetrics[n1][n2];
        RotationalBacktestConfig[][] configGrid = new RotationalBacktestConfig[n1][n2];

        for (OptimizationResult r : results) {
            int i1 = (int) Math.round((r.paramValues()[0] - p1.min()) / p1.step());
            int i2 = (int) Math.round((r.paramValues()[1] - p2.min()) / p2.step());
            i1 = Math.max(0, Math.min(i1, n1 - 1));
            i2 = Math.max(0, Math.min(i2, n2 - 1));
            scoreGrid[i1][i2] = score(r.metrics());
            metricsGrid[i1][i2] = r.metrics();
            configGrid[i1][i2] = r.config();
        }

        // 2D stability: average of self + 4 cardinal neighbors (or fewer at edges)
        double[][] stabGrid = new double[n1][n2];
        int bestI = 0, bestJ = 0;
        double bestStab = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n1; i++) {
            for (int j = 0; j < n2; j++) {
                double sum = scoreGrid[i][j];
                int cnt = 1;
                if (i > 0) { sum += scoreGrid[i - 1][j]; cnt++; }
                if (i < n1 - 1) { sum += scoreGrid[i + 1][j]; cnt++; }
                if (j > 0) { sum += scoreGrid[i][j - 1]; cnt++; }
                if (j < n2 - 1) { sum += scoreGrid[i][j + 1]; cnt++; }
                stabGrid[i][j] = sum / cnt;

                if (stabGrid[i][j] > bestStab) {
                    bestStab = stabGrid[i][j];
                    bestI = i;
                    bestJ = j;
                }
            }
        }

        // Log table
        log.info("");
        log.info("  {}", String.format("%-10s %-10s %8s %8s %8s %10s %7s %8s %8s",
                p1.name(), p2.name(), "Sortino", "Sharpe", "MaxDD%", "PnL", "Trades", "Score", "Stable"));
        log.info("  {}", "-".repeat(95));

        for (int i = 0; i < n1; i++) {
            for (int j = 0; j < n2; j++) {
                RotationalMetrics m = metricsGrid[i][j];
                if (m == null) continue;
                String marker = (i == bestI && j == bestJ) ? " <--" : "";
                log.info("  {}", String.format("%-10s %-10s %8.2f %8.2f %7.1f%% %10s %7d %8.2f %8.2f%s",
                        fmt(p1.valueAt(i)), fmt(p2.valueAt(j)),
                        m.getSortino(), m.getSharpe(),
                        m.getMaxDrawdown() * 100,
                        fmtPnl(m.getLongPnl() + m.getShortPnl()),
                        m.getTotalTrades(), scoreGrid[i][j], stabGrid[i][j], marker));
            }
        }

        log.info("");
        log.info("  >> Robust best: {}={} {}={} (stability={}, raw={})",
                p1.name(), fmt(p1.valueAt(bestI)),
                p2.name(), fmt(p2.valueAt(bestJ)),
                fmt(stabGrid[bestI][bestJ]), fmt(scoreGrid[bestI][bestJ]));
        logMetrics(metricsGrid[bestI][bestJ]);
        return configGrid[bestI][bestJ];
    }

    // ── Enum/discrete parameter optimization ─────────────────────────────

    @FunctionalInterface
    interface IndexedConfigBuilder {
        RotationalBacktestConfig build(int index);
    }

    @FunctionalInterface
    interface IndexedLabeler {
        String label(int index);
    }

    private static RotationalBacktestConfig optimizeEnumParam(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            String phaseLabel, String paramName,
            int count, IndexedLabeler labeler, IndexedConfigBuilder builder) throws Exception {

        log.info("==========================================================================================");
        log.info("  {}", phaseLabel);
        log.info("==========================================================================================");
        log.info("  Grid: {} values for {}", count, paramName);

        // Run all configs in parallel
        List<Object[]> results = Collections.synchronizedList(new ArrayList<>());
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ForkJoinPool pool = new ForkJoinPool(parallelism);

        try {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < count; i++) indices.add(i);

            pool.submit(() -> indices.parallelStream().forEach(idx -> {
                RotationalBacktestConfig cfg = builder.build(idx);
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                results.add(new Object[]{idx, cfg, m, labeler.label(idx)});
            })).get();
        } finally {
            pool.shutdown();
        }

        // Sort by index for stable ordering
        results.sort((a, b) -> Integer.compare((int) a[0], (int) b[0]));

        // Compute scores + stability
        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score((RotationalMetrics) results.get(i)[2]);
        }
        double[] stabScores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            stabScores[i] = stabilityScore(scores, i);
        }

        // Find best by stability
        int bestIdx = 0;
        double bestStab = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < results.size(); i++) {
            if (stabScores[i] > bestStab) {
                bestStab = stabScores[i];
                bestIdx = i;
            }
        }

        // Log
        log.info("");
        log.info("  {}", String.format("%-16s %8s %8s %8s %10s %7s %8s %8s",
                paramName, "Sortino", "Sharpe", "MaxDD%", "PnL", "Trades", "Score", "Stable"));
        log.info("  {}", "-".repeat(85));

        for (int i = 0; i < results.size(); i++) {
            RotationalMetrics m = (RotationalMetrics) results.get(i)[2];
            if (m == null) continue;
            String label = (String) results.get(i)[3];
            String marker = (i == bestIdx) ? " <-- BEST" : "";
            log.info("  {}", String.format("%-16s %8.2f %8.2f %7.1f%% %10s %7d %8.2f %8.2f%s",
                    label, m.getSortino(), m.getSharpe(),
                    m.getMaxDrawdown() * 100,
                    fmtPnl(m.getLongPnl() + m.getShortPnl()),
                    m.getTotalTrades(), scores[i], stabScores[i], marker));
        }

        log.info("");
        log.info("  >> Robust best {}: {} (stability={})",
                paramName, (String) results.get(bestIdx)[3], fmt(stabScores[bestIdx]));
        logMetrics((RotationalMetrics) results.get(bestIdx)[2]);
        return (RotationalBacktestConfig) results.get(bestIdx)[1];
    }

    // ── Trailing Stop optimization ───────────────────────────────────────

    private static RotationalBacktestConfig optimizeTrailingStop(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        log.info("==========================================================================================");
        log.info("  Phase 4: Trailing Stop");
        log.info("==========================================================================================");

        double[] multipliers = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
        RotationalBacktestConfig.StopBasis[] bases = {
                RotationalBacktestConfig.StopBasis.OR_RANGE,
                RotationalBacktestConfig.StopBasis.ATR
        };

        List<Object[]> results = new ArrayList<>();

        // Disabled
        var b = copyToBuilder(base);
        b.trailingStopEnabled(false);
        RotationalBacktestConfig cfg = b.build();
        RotationalMetrics m = runSingle(cfg, intraday, daily);
        results.add(new Object[]{cfg, m, "Disabled"});

        // Enabled combos
        for (RotationalBacktestConfig.StopBasis basis : bases) {
            for (double mult : multipliers) {
                var bb = copyToBuilder(base);
                bb.trailingStopEnabled(true);
                bb.trailingStopBasis(basis);
                bb.trailingStopMultiplier(mult);
                RotationalBacktestConfig c = bb.build();
                RotationalMetrics mm = runSingle(c, intraday, daily);
                results.add(new Object[]{c, mm, String.format("%.2fx %s", mult, basis)});
            }
        }

        // Stability scoring
        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score((RotationalMetrics) results.get(i)[1]);
        }
        double[] stabScores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            stabScores[i] = stabilityScore(scores, i);
        }

        int bestIdx = 0;
        double bestStab = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < results.size(); i++) {
            if (stabScores[i] > bestStab) {
                bestStab = stabScores[i];
                bestIdx = i;
            }
        }

        log.info("");
        log.info("  {}", String.format("%-20s %8s %8s %8s %10s %7s %8s %8s",
                "TrailingStop", "Sortino", "Sharpe", "MaxDD%", "PnL", "Trades", "Score", "Stable"));
        log.info("  {}", "-".repeat(85));

        for (int i = 0; i < results.size(); i++) {
            RotationalMetrics mm2 = (RotationalMetrics) results.get(i)[1];
            if (mm2 == null) continue;
            String marker = (i == bestIdx) ? " <-- BEST" : "";
            log.info("  {}", String.format("%-20s %8.2f %8.2f %7.1f%% %10s %7d %8.2f %8.2f%s",
                    results.get(i)[2], mm2.getSortino(), mm2.getSharpe(),
                    mm2.getMaxDrawdown() * 100,
                    fmtPnl(mm2.getLongPnl() + mm2.getShortPnl()),
                    mm2.getTotalTrades(), scores[i], stabScores[i], marker));
        }

        log.info("");
        log.info("  >> Robust best TrailingStop: {} (stability={})",
                results.get(bestIdx)[2], fmt(stabScores[bestIdx]));
        logMetrics((RotationalMetrics) results.get(bestIdx)[1]);
        return (RotationalBacktestConfig) results.get(bestIdx)[0];
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static void logMetrics(RotationalMetrics m) {
        if (m != null) {
            log.info("     Sortino={} Sharpe={} MaxDD={}% Trades={} PF={} PnL={}",
                    fmt(m.getSortino()), fmt(m.getSharpe()),
                    fmt(m.getMaxDrawdown() * 100),
                    m.getTotalTrades(), fmt(m.getProfitFactor()),
                    fmtPnl(m.getLongPnl() + m.getShortPnl()));
        }
        log.info("");
    }

    private static RotationalMetrics runSingle(
            RotationalBacktestConfig config,
            Map<String, Bars> intraday, Map<String, Bars> daily) {
        try {
            RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
            RotationalBacktestResult result = engine.run(intraday, daily, null);
            return result.getMetrics();
        } catch (Exception e) {
            log.warn("Run failed: {}", e.getMessage());
            return null;
        }
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

    private static String fmt(double value) { return String.format("%.2f", value); }

    private static String fmtPnl(double value) {
        if (Math.abs(value) >= 100_000) return String.format("%.2fL", value / 100_000);
        return String.format("%.0f", value);
    }
}
