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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive multi-phase parameter optimization for the Rotational ORB strategy.
 *
 * <p>Optimizes all parameters in logical groups using iterative refinement:</p>
 * <ol>
 *   <li><b>Phase 1 — Structural:</b> Stop multiplier x Target multiplier</li>
 *   <li><b>Phase 2 — Timing:</b> Entry cutoff x Exit time</li>
 *   <li><b>Phase 3 — Trailing stop:</b> Enabled/disabled x multiplier</li>
 *   <li><b>Phase 4 — Gap filter:</b> minGapAtr sweep</li>
 *   <li><b>Phase 5 — Picks:</b> picks count sweep</li>
 *   <li><b>Phase 6 — Re-entries:</b> maxReEntries sweep</li>
 *   <li><b>Phase 7 — OR IBS filter:</b> maxOrbIbs sweep</li>
 *   <li><b>Phase 8 — ATR scaling:</b> on/off</li>
 *   <li><b>Phase 9 — Refinement:</b> Fine-tune top parameters with smaller steps</li>
 * </ol>
 *
 * <p>Each phase takes the best config from the previous phase as its baseline.</p>
 */
public final class FullParameterOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(FullParameterOptimizationDriver.class);

    // Scoring: composite of Sharpe, Sortino, and stability
    // Penalize configs with < 500 trades (too few for statistical significance)
    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 100) return Double.NEGATIVE_INFINITY;
        // Primary: Sharpe. Secondary: Sortino bonus. Penalty for extreme drawdown.
        double s = m.getSharpe();
        s += 0.1 * m.getSortino();  // small bonus for better Sortino
        if (m.getMaxDrawdown() < -0.15) s -= 1.0;  // harsh penalty for > 15% DD
        if (m.getMaxDrawdown() < -0.10) s -= 0.5;  // penalty for > 10% DD
        return s;
    }

    // Stability-weighted score: penalizes parameters at extreme ends of their range
    // to prefer robust middle-of-the-road values
    private static double stabilityScore(RotationalMetrics m, int neighborWins) {
        double s = score(m);
        // Bonus if nearby parameter values also score well (plateau = stable)
        s += 0.05 * neighborWins;
        return s;
    }

    public static void main(String[] args) throws Exception {
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  Full Parameter Optimization — Rotational ORB Strategy");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
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
        log.info("BASELINE: Sharpe={} Sortino={} PnL={} MaxDD={}% Trades={} CAGR={}%",
                fmt(baseline.getSharpe()), fmt(baseline.getSortino()),
                fmtPnl(baseline.getLongPnl() + baseline.getShortPnl()),
                fmt(baseline.getMaxDrawdown() * 100), baseline.getTotalTrades(),
                fmt(baseline.getCagr() * 100));
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 1: Stop Multiplier x Target Multiplier
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 1: Stop Multiplier x Target Multiplier");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeTwoParams(best, intradayBars, dailyBars,
                new OptimizableParameter("StopMult", "stopMultiplier", 0.5, 2.0, 0.25),
                new OptimizableParameter("TargetMult", "targetMultiplier", 1.0, 5.0, 0.5));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 2: Entry Cutoff Time
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 2: Entry Cutoff Time");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        // Entry cutoff: 10:00, 10:30, 11:00, ..., 15:00 IST
        LocalTime[] cutoffTimes = {
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(12, 30),
                LocalTime.of(13, 0), LocalTime.of(13, 30),
                LocalTime.of(14, 0), LocalTime.of(14, 30),
                LocalTime.of(15, 0), LocalTime.of(15, 15)
        };
        best = optimizeEntryCutoff(best, intradayBars, dailyBars, cutoffTimes);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 3: Exit Time
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 3: Exit Time");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        // Exit time: null (market close), 14:30, 14:45, 15:00, 15:10, 15:15, 15:20, 15:25
        LocalTime[] exitTimes = {
                null, // market close
                LocalTime.of(14, 30), LocalTime.of(14, 45),
                LocalTime.of(15, 0), LocalTime.of(15, 10),
                LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)
        };
        best = optimizeExitTime(best, intradayBars, dailyBars, exitTimes);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 4: Trailing Stop
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 4: Trailing Stop (enabled/disabled x multiplier)");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeTrailingStop(best, intradayBars, dailyBars);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 5: Gap Filter
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 5: Min Gap ATR Filter");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeOneParam(best, intradayBars, dailyBars,
                new OptimizableParameter("MinGapATR", "minGapAtr", 0.0, 1.5, 0.10));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 6: Picks
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 6: Long Picks x Short Picks");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeOneParam(best, intradayBars, dailyBars,
                new OptimizableParameter("Picks", "picks", 1, 10, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 7: Re-entries
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 7: Max Re-Entries");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeOneParam(best, intradayBars, dailyBars,
                new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 8: OR IBS Filter
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 8: Max OR IBS Filter");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        // IBS sweeps: no filter (maxOrbIbs=null mapped as 1.0), 0.2, 0.3, ..., 0.9
        best = optimizeOrbIbs(best, intradayBars, dailyBars);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 9: ATR Scaling
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 9: ATR Scaling On/Off");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        best = optimizeOneParam(best, intradayBars, dailyBars,
                new OptimizableParameter("AtrScaling", "atrScaling", 0, 1, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 10: Refinement — fine-tune stop & target with smaller steps
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 10: Refinement — Fine-tune Stop x Target");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        double bestStop = best.getStopMultiplier();
        double bestTarget = best.getTargetMultiplier();
        best = optimizeTwoParams(best, intradayBars, dailyBars,
                new OptimizableParameter("StopMult", "stopMultiplier",
                        Math.max(0.25, bestStop - 0.25), bestStop + 0.25, 0.05),
                new OptimizableParameter("TargetMult", "targetMultiplier",
                        Math.max(0.5, bestTarget - 1.0), bestTarget + 1.0, 0.25));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 11: Refinement — fine-tune gap filter
        // ══════════════════════════════════════════════════════════════════
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  PHASE 11: Refinement — Fine-tune Gap Filter");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        double bestGap = best.getMinGapAtr() != null ? best.getMinGapAtr() : 0.0;
        best = optimizeOneParam(best, intradayBars, dailyBars,
                new OptimizableParameter("MinGapATR", "minGapAtr",
                        Math.max(0.0, bestGap - 0.20), bestGap + 0.20, 0.05));

        // ══════════════════════════════════════════════════════════════════
        //  FINAL RESULT
        // ══════════════════════════════════════════════════════════════════
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  OPTIMIZATION COMPLETE — FINAL OPTIMAL CONFIG");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info("{}", best);
        log.info("");

        // Run final validation
        RotationalMetrics finalMetrics = runSingle(best, intradayBars, dailyBars);
        log.info("  FINAL METRICS:");
        log.info("  Sharpe:        {}", fmt(finalMetrics.getSharpe()));
        log.info("  Sortino:       {}", fmt(finalMetrics.getSortino()));
        log.info("  CAGR:          {}%", fmt(finalMetrics.getCagr() * 100));
        log.info("  Max Drawdown:  {}%", fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("  Profit Factor: {}", fmt(finalMetrics.getProfitFactor()));
        log.info("  Win Rate:      {}%", fmt(finalMetrics.getWinRate() * 100));
        log.info("  Total Trades:  {}", finalMetrics.getTotalTrades());
        log.info("  Net PnL:       {}", fmtPnl(finalMetrics.getLongPnl() + finalMetrics.getShortPnl()));
        log.info("");

        // Compare to baseline
        log.info("  IMPROVEMENT OVER BASELINE:");
        log.info(String.format("  Sharpe:  %s -> %s (%+.2f)", fmt(baseline.getSharpe()), fmt(finalMetrics.getSharpe()),
                finalMetrics.getSharpe() - baseline.getSharpe()));
        log.info(String.format("  Sortino: %s -> %s (%+.2f)", fmt(baseline.getSortino()), fmt(finalMetrics.getSortino()),
                finalMetrics.getSortino() - baseline.getSortino()));
        log.info("  CAGR:    {}% -> {}%", fmt(baseline.getCagr() * 100), fmt(finalMetrics.getCagr() * 100));
        log.info("  MaxDD:   {}% -> {}%", fmt(baseline.getMaxDrawdown() * 100), fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
    }

    // ── Phase helpers ────────────────────────────────────────────────────

    private static RotationalBacktestConfig optimizeOneParam(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter param) throws Exception {

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        AtomicInteger done = new AtomicInteger(0);
        int total = param.gridSize();

        log.info("  Grid: {} points for {}", total, param.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(param),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 5 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        return pickBestAndLog(results, param.name());
    }

    private static RotationalBacktestConfig optimizeTwoParams(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter p1, OptimizableParameter p2) throws Exception {

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

        return pickBestAndLog2D(results, p1.name(), p2.name());
    }

    private static RotationalBacktestConfig optimizeEntryCutoff(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            LocalTime[] times) throws Exception {

        log.info("  Grid: {} entry cutoff times", times.length);
        List<ConfigResult> results = runConfigs(base, intraday, daily, idx -> {
            var b = copyToBuilder(base);
            b.entryCutoffTime(times[idx]);
            return b.build();
        }, times.length, i -> times[i].toString());

        return pickBestFromList(results, "EntryCutoff");
    }

    private static RotationalBacktestConfig optimizeExitTime(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            LocalTime[] times) throws Exception {

        log.info("  Grid: {} exit times", times.length);
        List<ConfigResult> results = runConfigs(base, intraday, daily, idx -> {
            var b = copyToBuilder(base);
            b.exitTime(times[idx]);
            return b.build();
        }, times.length, i -> times[i] != null ? times[i].toString() : "MarketClose");

        return pickBestFromList(results, "ExitTime");
    }

    private static RotationalBacktestConfig optimizeTrailingStop(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        // Test: disabled, then enabled with multipliers 0.5, 0.75, 1.0, 1.25, 1.5, 2.0
        // Also test both OR_RANGE and ATR basis
        double[] multipliers = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
        RotationalBacktestConfig.StopBasis[] bases = {
                RotationalBacktestConfig.StopBasis.OR_RANGE,
                RotationalBacktestConfig.StopBasis.ATR
        };

        int total = 1 + multipliers.length * bases.length; // 1 for disabled
        log.info("  Grid: {} configs (1 disabled + {} multipliers x {} bases)",
                total, multipliers.length, bases.length);

        List<ConfigResult> results = new ArrayList<>();

        // Disabled
        {
            var b = copyToBuilder(base);
            b.trailingStopEnabled(false);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            results.add(new ConfigResult(cfg, m, "Disabled"));
        }

        // Enabled with each combo
        for (RotationalBacktestConfig.StopBasis basis : bases) {
            for (double mult : multipliers) {
                var b = copyToBuilder(base);
                b.trailingStopEnabled(true);
                b.trailingStopBasis(basis);
                b.trailingStopMultiplier(mult);
                RotationalBacktestConfig cfg = b.build();
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                results.add(new ConfigResult(cfg, m, String.format("%.2fx %s", mult, basis)));
            }
        }

        return pickBestFromList(results, "TrailingStop");
    }

    private static RotationalBacktestConfig optimizeOrbIbs(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        // Test: no filter (null), then maxOrbIbs = 0.2, 0.3, ..., 0.9
        Double[] ibsValues = {null, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};

        log.info("  Grid: {} IBS values", ibsValues.length);
        List<ConfigResult> results = runConfigs(base, intraday, daily, idx -> {
            var b = copyToBuilder(base);
            b.maxOrbIbs(ibsValues[idx]);
            return b.build();
        }, ibsValues.length, i -> ibsValues[i] != null ? fmt(ibsValues[i]) : "None");

        return pickBestFromList(results, "MaxOrbIBS");
    }

    // ── Generic parallel execution ───────────────────────────────────────

    record ConfigResult(RotationalBacktestConfig config, RotationalMetrics metrics, String label) {}

    @FunctionalInterface
    interface ConfigBuilder {
        RotationalBacktestConfig build(int index);
    }

    @FunctionalInterface
    interface LabelProvider {
        String label(int index);
    }

    private static List<ConfigResult> runConfigs(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            ConfigBuilder builder, int count, LabelProvider labeler) throws Exception {

        List<ConfigResult> results = Collections.synchronizedList(new ArrayList<>(count));
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ForkJoinPool pool = new ForkJoinPool(parallelism);

        try {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < count; i++) indices.add(i);

            pool.submit(() -> indices.parallelStream().forEach(idx -> {
                RotationalBacktestConfig cfg = builder.build(idx);
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                results.add(new ConfigResult(cfg, m, labeler.label(idx)));
            })).get();
        } finally {
            pool.shutdown();
        }

        // Sort by label for stable display
        results.sort(Comparator.comparing(ConfigResult::label));
        return results;
    }

    // ── Result picking ───────────────────────────────────────────────────

    private static RotationalBacktestConfig pickBestAndLog(
            List<OptimizationResult> results, String paramName) {

        log.info("");
        log.info(String.format("  %-20s %8s %8s %8s %10s %7s %7s",
                paramName, "Sharpe", "Sortino", "MaxDD%", "PnL", "Trades", "Score"));
        log.info("  " + "─".repeat(80));

        OptimizationResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (OptimizationResult r : results) {
            RotationalMetrics m = r.metrics();
            double s = score(m);
            String marker = "";

            if (s > bestScore) {
                bestScore = s;
                best = r;
                marker = " <-- BEST";
            }

            if (m != null) {
                log.info(String.format("  %-20s %8.2f %8.2f %7.1f%% %10s %7d %7.2f%s",
                        fmt(r.paramValues()[0]), m.getSharpe(), m.getSortino(),
                        m.getMaxDrawdown() * 100,
                        fmtPnl(m.getLongPnl() + m.getShortPnl()), m.getTotalTrades(), s, marker));
            }
        }

        log.info("");
        log.info("  >> Best {}: {}", paramName, fmt(best.paramValues()[0]));
        logConfig(best.config(), best.metrics());
        return best.config();
    }

    private static RotationalBacktestConfig pickBestAndLog2D(
            List<OptimizationResult> results, String p1Name, String p2Name) {

        log.info("");
        log.info(String.format("  %-10s %-10s %8s %8s %8s %10s %7s %7s",
                p1Name, p2Name, "Sharpe", "Sortino", "MaxDD%", "PnL", "Trades", "Score"));
        log.info("  " + "─".repeat(85));

        OptimizationResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (OptimizationResult r : results) {
            RotationalMetrics m = r.metrics();
            double s = score(m);

            if (s > bestScore) {
                bestScore = s;
                best = r;
            }

            if (m != null) {
                String marker = (r == best) ? " <--" : "";
                log.info(String.format("  %-10s %-10s %8.2f %8.2f %7.1f%% %10s %7d %7.2f%s",
                        fmt(r.paramValues()[0]), fmt(r.paramValues()[1]),
                        m.getSharpe(), m.getSortino(),
                        m.getMaxDrawdown() * 100,
                        fmtPnl(m.getLongPnl() + m.getShortPnl()), m.getTotalTrades(), s, marker));
            }
        }

        log.info("");
        log.info("  >> Best: {}={} {}={}", p1Name, fmt(best.paramValues()[0]),
                p2Name, fmt(best.paramValues()[1]));
        logConfig(best.config(), best.metrics());
        return best.config();
    }

    private static RotationalBacktestConfig pickBestFromList(
            List<ConfigResult> results, String paramName) {

        log.info("");
        log.info(String.format("  %-20s %8s %8s %8s %10s %7s %7s",
                paramName, "Sharpe", "Sortino", "MaxDD%", "PnL", "Trades", "Score"));
        log.info("  " + "─".repeat(80));

        ConfigResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (ConfigResult r : results) {
            RotationalMetrics m = r.metrics();
            double s = score(m);
            String marker = "";

            if (s > bestScore) {
                bestScore = s;
                best = r;
                marker = " <-- BEST";
            }

            if (m != null) {
                log.info(String.format("  %-20s %8.2f %8.2f %7.1f%% %10s %7d %7.2f%s",
                        r.label(), m.getSharpe(), m.getSortino(),
                        m.getMaxDrawdown() * 100,
                        fmtPnl(m.getLongPnl() + m.getShortPnl()), m.getTotalTrades(), s, marker));
            }
        }

        log.info("");
        log.info("  >> Best {}: {}", paramName, best.label());
        logConfig(best.config(), best.metrics());
        return best.config();
    }

    private static void logConfig(RotationalBacktestConfig config, RotationalMetrics m) {
        if (m != null) {
            log.info("     Sharpe={} Sortino={} CAGR={}% MaxDD={}% Trades={} PF={}",
                    fmt(m.getSharpe()), fmt(m.getSortino()),
                    fmt(m.getCagr() * 100), fmt(m.getMaxDrawdown() * 100),
                    m.getTotalTrades(), fmt(m.getProfitFactor()));
        }
        log.info("");
    }

    // ── Engine runner ────────────────────────────────────────────────────

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

    // ── Config copy helper ───────────────────────────────────────────────

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

    // ── Formatting ───────────────────────────────────────────────────────

    private static String fmt(double value) { return String.format("%.2f", value); }

    private static String fmtPnl(double value) {
        if (Math.abs(value) >= 100_000) return String.format("%.2fL", value / 100_000);
        return String.format("%.0f", value);
    }
}
