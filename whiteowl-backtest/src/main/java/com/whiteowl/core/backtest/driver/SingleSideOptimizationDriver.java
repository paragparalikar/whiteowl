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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-scenario full parameter optimization for single-side ORB strategies.
 *
 * <p>Optimizes each of the 4 directional scenarios independently using
 * data-driven parameter ranges from feature analysis. Each scenario runs
 * through multi-phase optimization:</p>
 * <ol>
 *   <li>Phase 1 — Stop x Target multiplier</li>
 *   <li>Phase 2 — Entry cutoff time</li>
 *   <li>Phase 3 — Exit time</li>
 *   <li>Phase 4 — Trailing stop</li>
 *   <li>Phase 5 — Gap ATR filter (side-specific, data-driven ranges)</li>
 *   <li>Phase 6 — Picks (single side only)</li>
 *   <li>Phase 7 — RVOL filter (side-specific)</li>
 *   <li>Phase 8 — OR IBS filter</li>
 *   <li>Phase 9 — Re-entries</li>
 *   <li>Phase 10 — ATR scaling</li>
 *   <li>Phase 11 — Refinement of stop x target</li>
 *   <li>Phase 12 — Refinement of gap filter</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.SingleSideOptimizationDriver
 * </pre>
 */
public final class SingleSideOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(SingleSideOptimizationDriver.class);

    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 50) return Double.NEGATIVE_INFINITY;
        double s = m.getSharpe();
        s += 0.1 * m.getSortino();
        if (m.getMaxDrawdown() < -0.20) s -= 1.0;
        if (m.getMaxDrawdown() < -0.12) s -= 0.5;
        return s;
    }

    // ── Scenario definitions ─────────────────────────────────────────

    private record ScenarioSpec(
            String name,
            boolean isLong,
            RotationalBacktestConfig.GapDirectionMode gapMode,
            // Data-driven gap filter sweep ranges (from feature analysis)
            double gapMinFrom, double gapMinTo, double gapMinStep,
            double gapMaxFrom, double gapMaxTo, double gapMaxStep,
            // RVOL sweep
            double rvolMinFrom, double rvolMinTo, double rvolMinStep,
            // Picks sweep
            int picksFrom, int picksTo
    ) {}

    private static List<ScenarioSpec> buildScenarios() {
        return List.of(
                // Long-Aligned: gap-up stocks, long on OR-high break
                // Signed gapAtr is POSITIVE for these stocks.
                // Feature analysis: P10=0.02, median=0.14, P90=0.39, best=[1.10,1.30]
                new ScenarioSpec("Long-Aligned", true,
                        RotationalBacktestConfig.GapDirectionMode.ALIGNED,
                        0.0, 0.50, 0.05,     // minGapAtr sweep (positive, gap-up)
                        0.40, 2.0, 0.10,     // maxGapAtr sweep
                        0.50, 3.0, 0.25,     // minRvol sweep
                        1, 15),              // picks sweep

                // Long-Opposite: gap-down stocks, long on OR-high break
                // Signed gapAtr is NEGATIVE for these stocks.
                // Feature analysis: P10=-0.38, median=-0.08, P90=0.00, best=[-1.00,-0.80]
                new ScenarioSpec("Long-Opposite", true,
                        RotationalBacktestConfig.GapDirectionMode.OPPOSITE,
                        -2.0, -0.05, 0.10,   // minGapAtr sweep (negative, gap-down)
                        -0.50, 0.0, 0.05,    // maxGapAtr sweep (up to 0)
                        0.50, 3.0, 0.25,     // minRvol
                        1, 15),

                // Short-Aligned: gap-down stocks, short on OR-low break
                // Signed gapAtr is NEGATIVE for these stocks.
                // Feature analysis: P10=-0.36, median=-0.07, P90=0.00, best=[-1.40,-1.20]
                new ScenarioSpec("Short-Aligned", false,
                        RotationalBacktestConfig.GapDirectionMode.ALIGNED,
                        -2.0, -0.05, 0.10,   // minGapAtr sweep (negative, gap-down)
                        -0.50, 0.0, 0.05,    // maxGapAtr sweep (up to 0)
                        0.50, 3.0, 0.25,     // minRvol
                        1, 15),

                // Short-Opposite: gap-up stocks, short on OR-low break
                // Signed gapAtr is POSITIVE for these stocks.
                // Feature analysis: P10=0.02, median=0.14, P90=0.42, best=[1.20,1.40]
                new ScenarioSpec("Short-Opposite", false,
                        RotationalBacktestConfig.GapDirectionMode.OPPOSITE,
                        0.0, 0.50, 0.05,     // minGapAtr sweep (positive, gap-up)
                        0.40, 2.0, 0.10,     // maxGapAtr sweep
                        0.50, 3.0, 0.25,     // minRvol
                        1, 15)
        );
    }

    /**
     * Build a neutral starting config for a scenario — no aggressive filters.
     */
    private static RotationalBacktestConfig buildBaseConfig(ScenarioSpec s) {
        var builder = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 0))
                .exitTime(LocalTime.of(15, 20))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.0)
                .trailingStopEnabled(false)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.0)
                .slippage(0.001)
                .initialCapital(1_000_000)
                .maxReEntries(0)
                .atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(s.gapMode())
                // Explicitly clear default gap filter — side-specific filters are added later
                .minGapAtr(null)
                .maxGapAtr(null);

        if (s.isLong()) {
            builder.side(RotationalBacktestConfig.Side.LONG);
        } else {
            builder.side(RotationalBacktestConfig.Side.SHORT);
        }
        builder.picks(5);

        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Single-Side Parameter Optimization Driver");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // ── Load data ────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());
        log.info("");

        // ── Run each scenario ────────────────────────────────────────
        List<ScenarioSpec> scenarios = buildScenarios();
        List<FinalResult> finalResults = new ArrayList<>();

        for (ScenarioSpec scenario : scenarios) {
            log.info("");
            log.info("██████████████████████████████████████████████████████████████████████████████████████");
            log.info("  OPTIMIZING: {}", scenario.name());
            log.info("██████████████████████████████████████████████████████████████████████████████████████");
            log.info("");

            RotationalBacktestConfig best = buildBaseConfig(scenario);

            // Run baseline
            RotationalMetrics baseline = runSingle(best, intradayBars, dailyBars);
            log.info("BASELINE: Sharpe={} Sortino={} Trades={} CAGR={}% MaxDD={}%",
                    fmt(baseline.getSharpe()), fmt(baseline.getSortino()),
                    baseline.getTotalTrades(),
                    fmt(baseline.getCagr() * 100), fmt(baseline.getMaxDrawdown() * 100));
            log.info("");

            // ── Phase 1: Stop x Target ───────────────────────────────
            log.info("── Phase 1: Stop x Target ──────────────────────────────────");
            best = optimizeTwoParams(best, intradayBars, dailyBars,
                    new OptimizableParameter("StopMult", "stopMultiplier", 0.5, 2.0, 0.25),
                    new OptimizableParameter("TargetMult", "targetMultiplier", 1.0, 5.0, 0.5));

            // ── Phase 2: Entry Cutoff ────────────────────────────────
            log.info("── Phase 2: Entry Cutoff ───────────────────────────────────");
            best = optimizeEntryCutoff(best, intradayBars, dailyBars);

            // ── Phase 3: Exit Time ───────────────────────────────────
            log.info("── Phase 3: Exit Time ────────────────────────────────────");
            best = optimizeExitTime(best, intradayBars, dailyBars);

            // ── Phase 4: Trailing Stop ───────────────────────────────
            log.info("── Phase 4: Trailing Stop ──────────────────────────────────");
            best = optimizeTrailingStop(best, intradayBars, dailyBars);

            // ── Phase 5: Gap ATR Filter ──────────────────────────────
            log.info("── Phase 5: Gap ATR Filter ─────────────────────────────────");
            best = optimizeTwoParams(best, intradayBars, dailyBars,
                    new OptimizableParameter("MinGapATR", "minGapAtr",
                            scenario.gapMinFrom(), scenario.gapMinTo(), scenario.gapMinStep()),
                    new OptimizableParameter("MaxGapATR", "maxGapAtr",
                            scenario.gapMaxFrom(), scenario.gapMaxTo(), scenario.gapMaxStep()));

            // ── Phase 6: Picks ───────────────────────────────────────
            log.info("── Phase 6: Picks ──────────────────────────────────────────");
            best = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("Picks", "picks",
                            scenario.picksFrom(), scenario.picksTo(), 1));

            // ── Phase 7: RVOL Filter ─────────────────────────────────
            log.info("── Phase 7: RVOL Filter ──────────────────────────────────");
            best = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MinRVOL", "minOrbRvol",
                            scenario.rvolMinFrom(), scenario.rvolMinTo(), scenario.rvolMinStep()));

            // ── Phase 8: OR IBS Filter ───────────────────────────────
            log.info("── Phase 8: OR IBS Filter ────────────────────────────────");
            best = optimizeOrbIbs(best, intradayBars, dailyBars);

            // ── Phase 9: Re-entries ──────────────────────────────────
            log.info("── Phase 9: Re-entries ───────────────────────────────────");
            best = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));

            // ── Phase 10: ATR Scaling ────────────────────────────────
            log.info("── Phase 10: ATR Scaling ─────────────────────────────────");
            best = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("AtrScaling", "atrScaling", 0, 1, 1));

            // ── Phase 11: Refinement — Stop x Target ─────────────────
            log.info("── Phase 11: Refine Stop x Target ────────────────────────");
            double bestStop = best.getStopMultiplier();
            double bestTarget = best.getTargetMultiplier();
            best = optimizeTwoParams(best, intradayBars, dailyBars,
                    new OptimizableParameter("StopMult", "stopMultiplier",
                            Math.max(0.25, bestStop - 0.25), bestStop + 0.25, 0.05),
                    new OptimizableParameter("TargetMult", "targetMultiplier",
                            Math.max(0.5, bestTarget - 1.0), bestTarget + 1.0, 0.25));

            // ── Phase 12: Refinement — Gap Filter ────────────────────
            log.info("── Phase 12: Refine Gap Filter ───────────────────────────");
            Double currentGapMin = best.getMinGapAtr();
            double gapMinVal = currentGapMin != null ? currentGapMin : 0.0;
            // For negative-gap scenarios, allow negative refinement range
            double refineMin = gapMinVal - 0.15;
            double refineMax = gapMinVal + 0.15;
            // Ensure min <= max
            if (refineMin > refineMax) { double tmp = refineMin; refineMin = refineMax; refineMax = tmp; }
            best = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MinGapATR", "minGapAtr",
                            refineMin, refineMax, 0.025));

            // ── Final result ─────────────────────────────────────────
            RotationalMetrics finalMetrics = runSingle(best, intradayBars, dailyBars);
            finalResults.add(new FinalResult(scenario.name(), best, finalMetrics, baseline));

            log.info("");
            log.info("  ── {} OPTIMAL CONFIG ──", scenario.name());
            log.info("{}", best);
            log.info("");
            log.info(String.format("  Sharpe: %.2f  Sortino: %.2f  CAGR: %.1f%%  MaxDD: %.1f%%  Trades: %d  WR: %.1f%%  PnL: %s",
                    finalMetrics.getSharpe(), finalMetrics.getSortino(),
                    finalMetrics.getCagr() * 100, finalMetrics.getMaxDrawdown() * 100,
                    finalMetrics.getTotalTrades(), finalMetrics.getWinRate() * 100,
                    fmtPnl(finalMetrics.getLongPnl() + finalMetrics.getShortPnl())));
            log.info(String.format("  Baseline Sharpe: %.2f -> %.2f (%+.2f)",
                    baseline.getSharpe(), finalMetrics.getSharpe(),
                    finalMetrics.getSharpe() - baseline.getSharpe()));
            log.info("");
        }

        // ══════════════════════════════════════════════════════════════
        //  GRAND COMPARISON
        // ══════════════════════════════════════════════════════════════
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  OPTIMIZATION RESULTS — ALL SCENARIOS");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info(String.format("  %-20s %7s %7s %7s %7s %7s %7s %10s",
                "Scenario", "Sharpe", "Sortn", "CAGR%", "MaxDD%", "Trades", "WR%", "Net PnL"));
        log.info("  " + "-".repeat(82));

        for (FinalResult fr : finalResults) {
            RotationalMetrics m = fr.metrics();
            log.info(String.format("  %-20s %7.2f %7.2f %6.1f%% %6.1f%% %7d %6.1f%% %10s",
                    fr.name(), m.getSharpe(), m.getSortino(),
                    m.getCagr() * 100, m.getMaxDrawdown() * 100,
                    m.getTotalTrades(), m.getWinRate() * 100,
                    fmtPnl(m.getLongPnl() + m.getShortPnl())));
        }

        log.info("");
        log.info("  ── Key Config Parameters ──");
        for (FinalResult fr : finalResults) {
            RotationalBacktestConfig c = fr.config();
            log.info("  {}: stop={} target={} cutoff={} exit={} trail={} gap=[{},{}] rvol>={} ibs<={} picks={} reEntry={}",
                    fr.name(),
                    fmt(c.getStopMultiplier()), fmt(c.getTargetMultiplier()),
                    c.getEntryCutoffTime(), c.getExitTime(),
                    c.isTrailingStopEnabled() ? fmt(c.getTrailingStopMultiplier()) + "/" + c.getTrailingStopBasis() : "off",
                    c.getMinGapAtr() != null ? fmt(c.getMinGapAtr()) : "none",
                    c.getMaxGapAtr() != null ? fmt(c.getMaxGapAtr()) : "none",
                    c.getMinOrbRvol() != null ? fmt(c.getMinOrbRvol()) : "none",
                    c.getMaxOrbIbs() != null ? fmt(c.getMaxOrbIbs()) : "none",
                    c.getPicks(),
                    c.getMaxReEntries());
        }

        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
    }

    // ── Phase helpers ────────────────────────────────────────────────

    private static RotationalBacktestConfig optimizeOneParam(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter param) throws Exception {

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = param.gridSize();
        log.info("  Grid: {} points for {}", total, param.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(param),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 5 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        return pickBest(results, param.name());
    }

    private static RotationalBacktestConfig optimizeTwoParams(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter p1, OptimizableParameter p2) throws Exception {

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = p1.gridSize() * p2.gridSize();
        log.info("  Grid: {} x {} = {} points", p1.gridSize(), p2.gridSize(), total);

        List<OptimizationResult> results = engine.optimize(base, List.of(p1, p2),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 10 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        return pickBest(results, p1.name() + " x " + p2.name());
    }

    private static RotationalBacktestConfig optimizeEntryCutoff(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        LocalTime[] times = {
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(12, 30),
                LocalTime.of(13, 0), LocalTime.of(13, 30),
                LocalTime.of(14, 0), LocalTime.of(14, 30),
                LocalTime.of(15, 0), LocalTime.of(15, 15)
        };
        log.info("  Grid: {} entry cutoff times", times.length);

        double bestScore = Double.NEGATIVE_INFINITY;
        RotationalBacktestConfig bestConfig = base;

        for (LocalTime t : times) {
            var b = copyToBuilder(base);
            b.entryCutoffTime(t);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            log.info("    {} -> Sharpe={} Sortino={} Trades={} score={}",
                    t, fmt(m.getSharpe()), fmt(m.getSortino()), m.getTotalTrades(), fmt(s));
            if (s > bestScore) {
                bestScore = s;
                bestConfig = cfg;
            }
        }
        log.info("  BEST: {} (score={})", bestConfig.getEntryCutoffTime(), fmt(bestScore));
        return bestConfig;
    }

    private static RotationalBacktestConfig optimizeExitTime(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        LocalTime[] times = {null, LocalTime.of(15, 0), LocalTime.of(15, 10),
                LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)};
        log.info("  Grid: {} exit times", times.length);

        double bestScore = Double.NEGATIVE_INFINITY;
        RotationalBacktestConfig bestConfig = base;

        for (LocalTime t : times) {
            var b = copyToBuilder(base);
            b.exitTime(t);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            log.info("    {} -> Sharpe={} Sortino={} Trades={} score={}",
                    t != null ? t : "MktClose", fmt(m.getSharpe()), fmt(m.getSortino()),
                    m.getTotalTrades(), fmt(s));
            if (s > bestScore) {
                bestScore = s;
                bestConfig = cfg;
            }
        }
        log.info("  BEST: {} (score={})",
                bestConfig.getExitTime() != null ? bestConfig.getExitTime() : "MktClose",
                fmt(bestScore));
        return bestConfig;
    }

    private static RotationalBacktestConfig optimizeTrailingStop(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        double[] multipliers = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
        RotationalBacktestConfig.StopBasis[] bases = {
                RotationalBacktestConfig.StopBasis.OR_RANGE,
                RotationalBacktestConfig.StopBasis.ATR
        };
        log.info("  Grid: 1 disabled + {} enabled combos", multipliers.length * bases.length);

        double bestScore = Double.NEGATIVE_INFINITY;
        RotationalBacktestConfig bestConfig = base;

        // Disabled
        {
            var b = copyToBuilder(base);
            b.trailingStopEnabled(false);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            log.info("    Disabled -> Sharpe={} score={}", fmt(m.getSharpe()), fmt(s));
            if (s > bestScore) { bestScore = s; bestConfig = cfg; }
        }

        for (RotationalBacktestConfig.StopBasis sb : bases) {
            for (double mult : multipliers) {
                var b = copyToBuilder(base);
                b.trailingStopEnabled(true).trailingStopBasis(sb).trailingStopMultiplier(mult);
                RotationalBacktestConfig cfg = b.build();
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                double s = score(m);
                log.info("    {} x {} -> Sharpe={} score={}", sb, fmt(mult),
                        fmt(m.getSharpe()), fmt(s));
                if (s > bestScore) { bestScore = s; bestConfig = cfg; }
            }
        }
        log.info("  BEST: trail={} (score={})",
                bestConfig.isTrailingStopEnabled()
                        ? fmt(bestConfig.getTrailingStopMultiplier()) + "/" + bestConfig.getTrailingStopBasis()
                        : "off",
                fmt(bestScore));
        return bestConfig;
    }

    private static RotationalBacktestConfig optimizeOrbIbs(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        Double[] ibsValues = {null, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
        log.info("  Grid: {} IBS values", ibsValues.length);

        double bestScore = Double.NEGATIVE_INFINITY;
        RotationalBacktestConfig bestConfig = base;

        for (Double ibs : ibsValues) {
            var b = copyToBuilder(base);
            b.maxOrbIbs(ibs);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            log.info("    maxIBS={} -> Sharpe={} Trades={} score={}",
                    ibs != null ? fmt(ibs) : "none", fmt(m.getSharpe()),
                    m.getTotalTrades(), fmt(s));
            if (s > bestScore) { bestScore = s; bestConfig = cfg; }
        }
        log.info("  BEST: maxIBS={} (score={})",
                bestConfig.getMaxOrbIbs() != null ? fmt(bestConfig.getMaxOrbIbs()) : "none",
                fmt(bestScore));
        return bestConfig;
    }

    private static RotationalBacktestConfig pickBest(List<OptimizationResult> results,
                                                      String paramName) {
        OptimizationResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (OptimizationResult r : results) {
            double s = score(r.metrics());
            if (s > bestScore) {
                bestScore = s;
                best = r;
            }
        }
        if (best == null) throw new IllegalStateException("No valid results for " + paramName);

        log.info("  BEST {}: params={} Sharpe={} Sortino={} Trades={} score={}",
                paramName, Arrays.toString(best.paramValues()),
                fmt(best.metrics().getSharpe()), fmt(best.metrics().getSortino()),
                best.metrics().getTotalTrades(), fmt(bestScore));
        return best.config();
    }

    // ── Utilities ────────────────────────────────────────────────────

    private static RotationalMetrics runSingle(RotationalBacktestConfig config,
                                                Map<String, Bars> intraday,
                                                Map<String, Bars> daily) {
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intraday, daily, null);
        return result.getMetrics();
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

    private record FinalResult(String name, RotationalBacktestConfig config,
                               RotationalMetrics metrics, RotationalMetrics baseline) {}

    private static String fmt(double value) { return String.format("%.2f", value); }

    private static String fmtPnl(double value) {
        if (Math.abs(value) >= 100_000) return String.format("%.2fL", value / 100_000);
        return String.format("%.0f", value);
    }
}
