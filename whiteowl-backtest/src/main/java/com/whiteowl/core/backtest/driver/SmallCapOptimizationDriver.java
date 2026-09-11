package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.optimizer.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.universe.OrbCharacteristicFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntFunction;

/**
 * Full 11-phase parameter optimization for ₹10L capital at 0.25% slippage.
 *
 * <h3>Setup</h3>
 * <ul>
 *   <li>Universe: top-1000-turnover pool filtered to ≥ 100L (₹1 Cr) avg daily turnover,
 *       then ranked by composite ORB score (Spread%, ATR%, Price↓, Volume↓).
 *       At ₹10L / 8 positions ≈ ₹1.25L per trade, a 100L ADV stock has 13× coverage
 *       — well within 0.25% market-impact budget.</li>
 *   <li>Capital: ₹10L (₹1,000,000)</li>
 *   <li>Slippage: 0.25% (0.0025) — realistic for ₹10L scale</li>
 * </ul>
 *
 * <h3>Optimization phases</h3>
 * <ol>
 *   <li>Stop × Target multiplier (coarse)</li>
 *   <li>Entry cutoff time</li>
 *   <li>Exit time</li>
 *   <li>Trailing stop (basis + multiplier)</li>
 *   <li>Min gap ATR filter (coarse)</li>
 *   <li>Long picks × Short picks</li>
 *   <li>Max re-entries</li>
 *   <li>Max OR IBS filter</li>
 *   <li>ATR scaling on/off</li>
 *   <li>Stop × Target refinement (fine)</li>
 *   <li>Min gap ATR refinement (fine)</li>
 * </ol>
 *
 * <p>Each phase's best config and all per-phase sweep results are captured and
 * written to an HTML report at the end.</p>
 */
public final class SmallCapOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(SmallCapOptimizationDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ── Live-trading parameters ───────────────────────────────────────
    static final double CAPITAL       = 1_000_000.0;  // ₹10L
    static final double SLIPPAGE      = 0.0025;        // 0.25%
    static final double ADV_FLOOR_L   = 100.0;         // 100L = ₹1 Cr ADV floor

    static final String SOURCE_GROUP  = "ORB Universe - Top 1000 Turnover";
    static final String LIQUID_GROUP  = "ORB SmallCap 1Cr Optimized";

    // ── Phase result tracking ─────────────────────────────────────────

    record PhaseResult(
            int phaseNum, String phaseName,
            List<SweepPoint> sweepPoints,
            RotationalBacktestConfig bestConfig,
            RotationalMetrics bestMetrics
    ) {}

    record SweepPoint(String label, RotationalBacktestConfig config, RotationalMetrics metrics) {}

    private static final List<PhaseResult> allPhases = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Full Parameter Optimization — ₹10L Capital / Small-Cap ORB Universe");
        log.info("  Capital: ₹10L  |  Slippage: 0.25%  |  ADV floor: {}L (₹{} Cr)",
                (long) ADV_FLOOR_L, (long)(ADV_FLOOR_L / 100));
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        BarsRepository barsRepo = new FileBarsRepository();

        // ── Step 1: Build 1 Cr ADV filtered universe ─────────────────
        log.info("Step 1: Building ₹1 Cr ADV filtered universe from '{}'", SOURCE_GROUP);
        List<String> sourceIds = RotationalOrbDriver.loadGroupScrips(SOURCE_GROUP);
        Map<String, Bars> allDaily = RotationalOrbDriver.loadIntradayBars(barsRepo, sourceIds, Timeframe.DAILY);

        var filterCfg = OrbCharacteristicFilter.FilterConfig.builder()
                .minTurnoverLakhs(ADV_FLOOR_L).minBars(100).build();
        List<OrbCharacteristicFilter.ScoredSymbol> ranked =
                new OrbCharacteristicFilter(filterCfg).scoreAndRank(allDaily);

        log.info("  {} / {} stocks pass ≥ {}L turnover floor", ranked.size(), sourceIds.size(), (long) ADV_FLOOR_L);
        log.info("  Top 15 by composite ORB score:");
        log.info("  {:>4}  {:<22} {:>7}  {:>7}  {:>7}  {:>10}", "Rank", "Symbol", "Score", "Spread%", "ATR%", "TO(L)");
        for (int i = 0; i < Math.min(15, ranked.size()); i++) {
            var s = ranked.get(i);
            log.info("  {:>4}  {:<22} {:>7.4f}  {:>6.2f}%  {:>6.2f}%  {:>10.0f}",
                    i+1, s.symbol(), s.compositeScore(), s.avgSpreadPct(), s.avgAtrPct(), s.avgTurnoverLakhs());
        }
        log.info("");

        // Save group file
        Path groupsDir = Path.of(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + java.io.File.separator + ".whiteowl"),
                "data", "groups");
        groupsDir.toFile().mkdirs();
        Path groupFile = groupsDir.resolve(LIQUID_GROUP + ".csv");
        Files.write(groupFile, ranked.stream().map(OrbCharacteristicFilter.ScoredSymbol::symbol).toList());
        log.info("  Saved universe '{}' → {} stocks → {}", LIQUID_GROUP, ranked.size(), groupFile);
        log.info("");

        // ── Step 2: Load intraday bars for optimization ───────────────
        log.info("Step 2: Loading intraday bars...");
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(LIQUID_GROUP);
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars    = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("  {} intraday, {} daily scrips loaded", intradayBars.size(), dailyBars.size());
        log.info("  Cores: {}", Runtime.getRuntime().availableProcessors());
        log.info("");

        // ── Starting config ───────────────────────────────────────────
        RotationalBacktestConfig start = RotationalBacktestConfig.builder()
                .universeGroupName(LIQUID_GROUP)
                .openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50).maxGapAtr(null)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(1.0)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(3.0)
                .trailingStopEnabled(false)
                .entryCutoffTime(LocalTime.of(14, 0))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(SLIPPAGE).initialCapital(CAPITAL)
                .atrScaling(false).maxReEntries(0)
                .build();

        RotationalMetrics baselineMetrics = runSingle(start, intradayBars, dailyBars);
        logMetrics("BASELINE", baselineMetrics);
        log.info("");

        RotationalBacktestConfig best = start;

        // ══════════════════════════════════════════════════════════════
        //  PHASE 1 — Stop × Target (coarse)
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 1: Stop Multiplier × Target Multiplier (coarse) ════");
        {
            var results = optimizeTwoParams(best, intradayBars, dailyBars,
                    new OptimizableParameter("StopMult",   "stopMultiplier",   0.50, 2.00, 0.25),
                    new OptimizableParameter("TargetMult", "targetMultiplier", 1.00, 5.00, 0.50));
            best = recordPhase(1, "Stop × Target (coarse)", results, "StopMult", "TargetMult");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 2 — Entry Cutoff Time
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 2: Entry Cutoff Time ════");
        {
            LocalTime[] times = {
                    LocalTime.of(10, 0), LocalTime.of(10, 30),
                    LocalTime.of(11, 0), LocalTime.of(11, 30),
                    LocalTime.of(12, 0), LocalTime.of(12, 30),
                    LocalTime.of(13, 0), LocalTime.of(13, 30),
                    LocalTime.of(14, 0), LocalTime.of(14, 30),
                    LocalTime.of(15, 0), LocalTime.of(15, 15)
            };
            final var base2 = best;
            var pts = sweepSeries(best, intradayBars, dailyBars,
                    i -> copyToBuilder(base2).entryCutoffTime(times[i]).build(),
                    times.length, i -> times[i].toString());
            best = recordPhaseSeries(2, "Entry Cutoff Time", pts);
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 3 — Exit Time
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 3: Exit Time ════");
        {
            LocalTime[] times = {
                    null,
                    LocalTime.of(14, 30), LocalTime.of(14, 45),
                    LocalTime.of(15, 0),  LocalTime.of(15, 10),
                    LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)
            };
            final var base3 = best;
            var pts = sweepSeries(best, intradayBars, dailyBars,
                    i -> copyToBuilder(base3).exitTime(times[i]).build(),
                    times.length, i -> times[i] != null ? times[i].toString() : "MarketClose");
            best = recordPhaseSeries(3, "Exit Time", pts);
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 4 — Trailing Stop
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 4: Trailing Stop ════");
        {
            double[] mults = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
            RotationalBacktestConfig.StopBasis[] bases = {
                    RotationalBacktestConfig.StopBasis.OR_RANGE,
                    RotationalBacktestConfig.StopBasis.ATR
            };
            List<SweepPoint> pts = new ArrayList<>();
            // Disabled
            {
                var cfg = copyToBuilder(best).trailingStopEnabled(false).build();
                pts.add(new SweepPoint("Disabled", cfg, runSingle(cfg, intradayBars, dailyBars)));
            }
            // Enabled combos
            for (var basis : bases) {
                for (double m : mults) {
                    var cfg = copyToBuilder(best).trailingStopEnabled(true)
                            .trailingStopBasis(basis).trailingStopMultiplier(m).build();
                    pts.add(new SweepPoint(basis.name() + "x" + m, cfg, runSingle(cfg, intradayBars, dailyBars)));
                }
            }
            best = recordPhaseSeries(4, "Trailing Stop", pts);
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 5 — Min Gap ATR (coarse)
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 5: Min Gap ATR filter (coarse) ════");
        {
            var results = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MinGapATR", "minGapAtr", 0.0, 1.5, 0.10));
            best = recordPhase(5, "Min Gap ATR (coarse)", results, "MinGapATR");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 6 — Long × Short picks
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 6: Picks ════");
        {
            var results = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("Picks", "picks", 1, 10, 1));
            best = recordPhase(6, "Picks", results, "Picks");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 7 — Max re-entries
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 7: Max Re-Entries ════");
        {
            var results = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));
            best = recordPhase(7, "Max Re-Entries", results, "MaxReEntries");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 8 — Max OR IBS filter
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 8: Max OR IBS Filter ════");
        {
            // null (no filter) + 0.2 … 0.9
            List<SweepPoint> pts = new ArrayList<>();
            {
                var cfg = copyToBuilder(best).maxOrbIbs(null).build();
                pts.add(new SweepPoint("No filter", cfg, runSingle(cfg, intradayBars, dailyBars)));
            }
            for (double ibs = 0.2; ibs <= 0.95; ibs += 0.10) {
                double v = Math.round(ibs * 10) / 10.0;
                var cfg = copyToBuilder(best).maxOrbIbs(v).build();
                pts.add(new SweepPoint(String.format("%.1f", v), cfg,
                        runSingle(cfg, intradayBars, dailyBars)));
            }
            best = recordPhaseSeries(8, "Max OR IBS Filter", pts);
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 9 — ATR Scaling
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 9: ATR Scaling On/Off ════");
        {
            var results = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("AtrScaling", "atrScaling", 0, 1, 1));
            best = recordPhase(9, "ATR Scaling", results, "AtrScaling");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 10 — Stop × Target refinement (fine)
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 10: Stop × Target (fine refinement) ════");
        {
            double bs = best.getStopMultiplier(), bt = best.getTargetMultiplier();
            var results = optimizeTwoParams(best, intradayBars, dailyBars,
                    new OptimizableParameter("StopMult",   "stopMultiplier",
                            Math.max(0.25, bs - 0.25), bs + 0.25, 0.05),
                    new OptimizableParameter("TargetMult", "targetMultiplier",
                            Math.max(0.50, bt - 1.00), bt + 1.00, 0.25));
            best = recordPhase(10, "Stop × Target (fine)", results, "StopMult", "TargetMult");
        }

        // ══════════════════════════════════════════════════════════════
        //  PHASE 11 — Min Gap ATR refinement (fine)
        // ══════════════════════════════════════════════════════════════
        log.info("════ PHASE 11: Min Gap ATR (fine refinement) ════");
        {
            double bg = best.getMinGapAtr() != null ? best.getMinGapAtr() : 0.0;
            var results = optimizeOneParam(best, intradayBars, dailyBars,
                    new OptimizableParameter("MinGapATR", "minGapAtr",
                            Math.max(0.0, bg - 0.20), bg + 0.20, 0.05));
            best = recordPhase(11, "Min Gap ATR (fine)", results, "MinGapATR");
        }

        // ── Final validation ──────────────────────────────────────────
        log.info("");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  OPTIMIZATION COMPLETE");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info("  Final config: {}", best);
        log.info("");

        // Run full backtest to get equity + drawdown curves for the report
        log.info("  Running final full backtest for equity/drawdown curves...");
        RotationalBacktestResult finalResult = runFull(best, intradayBars, dailyBars);
        RotationalMetrics finalMetrics = finalResult != null ? finalResult.getMetrics() : null;
        logMetrics("FINAL OPTIMAL", finalMetrics);
        log.info("");
        log.info("  IMPROVEMENT vs BASELINE:");
        if (finalMetrics != null) {
            log.info("  {}", String.format("  Sharpe:  %.2f -> %.2f (%+.2f)",
                    baselineMetrics.getSharpe(), finalMetrics.getSharpe(),
                    finalMetrics.getSharpe() - baselineMetrics.getSharpe()));
            log.info("  {}", String.format("  Sortino: %.2f -> %.2f (%+.2f)",
                    baselineMetrics.getSortino(), finalMetrics.getSortino(),
                    finalMetrics.getSortino() - baselineMetrics.getSortino()));
            log.info("  {}", String.format("  CAGR:    %.1f%% -> %.1f%%",
                    baselineMetrics.getCagr() * 100, finalMetrics.getCagr() * 100));
            log.info("  {}", String.format("  MaxDD:   %.1f%% -> %.1f%%",
                    baselineMetrics.getMaxDrawdown() * 100, finalMetrics.getMaxDrawdown() * 100));
        }

        // ── HTML Report ───────────────────────────────────────────────
        log.info("");
        log.info("Generating HTML report...");
        Path reportPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports",
                "smallcap_optimization.html");
        reportPath.getParent().toFile().mkdirs();
        generateHtmlReport(reportPath, ranked, baselineMetrics, best, finalMetrics, finalResult, allPhases);
        log.info("Report: {}", reportPath.toAbsolutePath());
        log.info("DONE");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Phase helpers
    // ══════════════════════════════════════════════════════════════════

    /** Run 1-param or 2-param grid optimization, return sorted results. */
    private static List<OptimizationResult> optimizeOneParam(
            RotationalBacktestConfig base, Map<String, Bars> intra, Map<String, Bars> daily,
            OptimizableParameter p) throws Exception {
        log.info("  Grid: {} points for {}", p.gridSize(), p.name());
        return new ParameterOptimizationEngine().optimize(base, List.of(p), intra, daily,
                (done, tot, latest) -> { if (done % 5 == 0 || done == tot) log.info("  {}/{}", done, tot); });
    }

    private static List<OptimizationResult> optimizeTwoParams(
            RotationalBacktestConfig base, Map<String, Bars> intra, Map<String, Bars> daily,
            OptimizableParameter p1, OptimizableParameter p2) throws Exception {
        int total = p1.gridSize() * p2.gridSize();
        log.info("  Grid: {} × {} = {} points ({} × {})", p1.gridSize(), p2.gridSize(), total, p1.name(), p2.name());
        return new ParameterOptimizationEngine().optimize(base, List.of(p1, p2), intra, daily,
                (done, tot, latest) -> { if (done % 10 == 0 || done == tot) log.info("  {}/{}", done, tot); });
    }

    /** Sweep a series of hand-crafted configs (for times, trailing-stop, IBS). */
    private static List<SweepPoint> sweepSeries(
            RotationalBacktestConfig base, Map<String, Bars> intra, Map<String, Bars> daily,
            IntFunction<RotationalBacktestConfig> factory, int count,
            IntFunction<String> labeller) throws Exception {
        List<SweepPoint> pts = new ArrayList<>();
        log.info("  Grid: {} configs", count);
        for (int i = 0; i < count; i++) {
            RotationalBacktestConfig cfg = factory.apply(i);
            pts.add(new SweepPoint(labeller.apply(i), cfg, runSingle(cfg, intra, daily)));
        }
        return pts;
    }

    /** Record a phase from OptimizationResult list (1 or 2 params). */
    private static RotationalBacktestConfig recordPhase(int num, String name,
            List<OptimizationResult> results, String... paramNames) {
        List<SweepPoint> pts = new ArrayList<>();
        for (var r : results) {
            String lbl;
            if (paramNames.length == 2)
                lbl = String.format("%.2f/%.2f", r.paramValues()[0], r.paramValues()[1]);
            else
                lbl = String.format("%.2f", r.paramValues()[0]);
            pts.add(new SweepPoint(lbl, r.config(), r.metrics()));
        }
        return recordPhaseSeries(num, name, pts);
    }

    /**
     * Record a phase from a SweepPoint list.
     *
     * <p>Selection philosophy: prefer the most ROBUST/STABLE value rather than the raw peak.
     * A "plateau winner" is a point whose score is within 10% of the maximum AND whose
     * immediate neighbours are also in the plateau. If the raw peak is an isolated spike
     * (no in-plateau neighbours) we skip it in favour of the best plateau member — this
     * deliberately sacrifices a few bps of back-test edge to avoid over-fitting.</p>
     */
    private static RotationalBacktestConfig recordPhaseSeries(int num, String name,
            List<SweepPoint> pts) {

        List<SweepPoint> valid = pts.stream()
                .filter(p -> p.metrics() != null && score(p.metrics()) > Double.NEGATIVE_INFINITY)
                .toList();
        if (valid.isEmpty()) {
            SweepPoint fallback = pts.get(0);
            allPhases.add(new PhaseResult(num, name, pts, fallback.config(), fallback.metrics()));
            log.info("  Phase {} -- no valid runs, using fallback", num);
            log.info("");
            return fallback.config();
        }

        double maxScore  = valid.stream().mapToDouble(p -> score(p.metrics())).max().orElse(0);
        double threshold = maxScore * 0.90; // top-10% plateau band

        // Count in-plateau neighbours (±2 positions in sweep order) for each candidate
        List<SweepPoint> allList = new ArrayList<>(pts);
        SweepPoint robustPt        = null;
        int        bestNeighbours  = -1;

        for (SweepPoint candidate : valid) {
            if (score(candidate.metrics()) < threshold) continue;
            int idx = allList.indexOf(candidate);
            int neighbours = 0;
            for (int d = -2; d <= 2; d++) {
                if (d == 0) continue;
                int ni = idx + d;
                if (ni < 0 || ni >= allList.size()) continue;
                SweepPoint n = allList.get(ni);
                if (n.metrics() != null && score(n.metrics()) >= threshold) neighbours++;
            }
            if (neighbours > bestNeighbours ||
                    (neighbours == bestNeighbours
                            && robustPt != null
                            && score(candidate.metrics()) > score(robustPt.metrics()))) {
                bestNeighbours = neighbours;
                robustPt = candidate;
            }
        }
        if (robustPt == null) robustPt = valid.get(0);

        // If robust pick IS the isolated raw peak (no in-plateau immediate neighbours),
        // prefer the next-best plateau member to avoid chasing a single spike.
        boolean isIsolatedPeak =
                Math.abs(score(robustPt.metrics()) - maxScore) < 0.001 && bestNeighbours == 0;
        if (isIsolatedPeak && valid.size() > 1) {
            final SweepPoint peak = robustPt;
            SweepPoint alternative = valid.stream()
                    .filter(p -> p != peak && score(p.metrics()) >= threshold)
                    .max(Comparator.comparingDouble(p -> score(p.metrics())))
                    .orElse(null);
            if (alternative != null) {
                log.info("  Phase {} -- raw peak [{}] is isolated spike; using robust alternative [{}]",
                        num, peak.label(), alternative.label());
                robustPt = alternative;
            }
        }

        allPhases.add(new PhaseResult(num, name, pts, robustPt.config(), robustPt.metrics()));
        log.info("  {}", String.format("  Phase %d plateau: picked [%s]  neighbours=%d  score=%.2f  peak=%.2f",
                num, robustPt.label(), bestNeighbours, score(robustPt.metrics()), maxScore));
        logMetrics("Phase " + num + " robust [" + robustPt.label() + "]", robustPt.metrics());
        log.info("");
        return robustPt.config();
    }

    // ── Score function (mirrors FullParameterOptimizationDriver) ─────

    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 100) return Double.NEGATIVE_INFINITY;
        double s = m.getSharpe() + 0.1 * m.getSortino();
        if (m.getMaxDrawdown() < -0.15) s -= 1.0;
        if (m.getMaxDrawdown() < -0.10) s -= 0.5;
        return s;
    }

    // ── runSingle ────────────────────────────────────────────────────

    private static RotationalMetrics runSingle(RotationalBacktestConfig cfg,
            Map<String, Bars> intra, Map<String, Bars> daily) {
        try {
            return new RotationalBacktestEngine(cfg).run(intra, daily, null).getMetrics();
        } catch (Exception e) {
            log.warn("Run failed: {}", e.getMessage());
            return null;
        }
    }

    /** Like runSingle but returns the full result (needed for equity/drawdown curves). */
    private static RotationalBacktestResult runFull(RotationalBacktestConfig cfg,
            Map<String, Bars> intra, Map<String, Bars> daily) {
        try {
            return new RotationalBacktestEngine(cfg).run(intra, daily, null);
        } catch (Exception e) {
            log.warn("Full run failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Log helpers ───────────────────────────────────────────────────

    private static void logMetrics(String label, RotationalMetrics m) {
        if (m == null) { log.info("  {}: null (run failed)", label); return; }
        log.info("  {}", String.format(
                "  %s: CAGR=%.1f%%  Sharpe=%.2f  Sortino=%.2f  WinRate=%.1f%%  PF=%.2f  MaxDD=%.1f%%  Trades=%d",
                label, m.getCagr() * 100, m.getSharpe(), m.getSortino(),
                m.getWinRate() * 100, m.getProfitFactor(),
                Math.abs(m.getMaxDrawdown()) * 100, m.getTotalTrades()));
    }

    // ── copyToBuilder ─────────────────────────────────────────────────

    static RotationalBacktestConfig.RotationalBacktestConfigBuilder copyToBuilder(
            RotationalBacktestConfig src) {
        return RotationalBacktestConfig.builder()
                .universeGroupName(src.getUniverseGroupName())
                .startDate(src.getStartDate())
                .endDate(src.getEndDate())
                .openingRangeMinutes(src.getOpeningRangeMinutes())
                .barMinutes(src.getBarMinutes())
                .side(src.getSide())
                .minGapAtr(src.getMinGapAtr()).maxGapAtr(src.getMaxGapAtr())
                .minOrAtr(src.getMinOrAtr()).maxOrAtr(src.getMaxOrAtr())
                .minOrbRvol(src.getMinOrbRvol()).maxOrbRvol(src.getMaxOrbRvol())
                .minRsRank(src.getMinRsRank()).maxRsRank(src.getMaxRsRank())
                .minOrbIbs(src.getMinOrbIbs()).maxOrbIbs(src.getMaxOrbIbs())
                .minOrBodyPct(src.getMinOrBodyPct()).maxOrBodyPct(src.getMaxOrBodyPct())
                .minOrSpreadPct(src.getMinOrSpreadPct()).maxOrSpreadPct(src.getMaxOrSpreadPct())
                .gapDirectionMode(src.getGapDirectionMode())
                .entryMethod(src.getEntryMethod())
                .maxReEntries(src.getMaxReEntries())
                .picks(src.getPicks())
                .rankerType(src.getRankerType())
                .entryCutoffTime(src.getEntryCutoffTime())
                .exitTime(src.getExitTime())
                .stopBasis(src.getStopBasis()).stopMultiplier(src.getStopMultiplier())
                .trailingStopEnabled(src.isTrailingStopEnabled())
                .trailingStopBasis(src.getTrailingStopBasis())
                .trailingStopMultiplier(src.getTrailingStopMultiplier())
                .targetEnabled(src.isTargetEnabled())
                .targetBasis(src.getTargetBasis()).targetMultiplier(src.getTargetMultiplier())
                .initialCapital(src.getInitialCapital())
                .slippage(src.getSlippage())
                .atrScaling(src.isAtrScaling());
    }

    // ══════════════════════════════════════════════════════════════════
    //  HTML Report
    // ══════════════════════════════════════════════════════════════════

    private static void generateHtmlReport(
            Path out,
            List<OrbCharacteristicFilter.ScoredSymbol> ranked,
            RotationalMetrics baseline,
            RotationalBacktestConfig finalCfg,
            RotationalMetrics finalMetrics,
            RotationalBacktestResult finalResult,
            List<PhaseResult> phases) throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            // ── Head ──────────────────────────────────────────────────
            w.write("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
            w.write("<title>ORB Optimization — ₹10L Small-Cap Universe</title>");
            w.write("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>");
            w.write("<style>" + CSS + "</style></head><body>");

            // ── Header ────────────────────────────────────────────────
            w.write("<div class=\"page-header\">");
            w.write("<h1>Full Parameter Optimization</h1>");
            w.write("<p class=\"subtitle\">Universe: ₹1 Cr ADV-Gated ORB Pool &nbsp;|&nbsp; ");
            w.write("Capital: ₹10L &nbsp;|&nbsp; Slippage: 0.25% &nbsp;|&nbsp; ");
            w.write(ranked.size() + " stocks &nbsp;|&nbsp; ");
            w.write("Generated: " + LocalDateTime.now(IST).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " IST</p></div>");

            // ── KPI row ───────────────────────────────────────────────
            w.write("<div class=\"kpi-row\">");
            kpi(w, "CAGR",     fmt(finalMetrics.getCagr() * 100) + "%",
                    delta(baseline.getCagr(), finalMetrics.getCagr(), 100, "%"));
            kpi(w, "Sharpe",   fmt(finalMetrics.getSharpe()),
                    delta(baseline.getSharpe(), finalMetrics.getSharpe(), 1, ""));
            kpi(w, "Sortino",  fmt(finalMetrics.getSortino()),
                    delta(baseline.getSortino(), finalMetrics.getSortino(), 1, ""));
            kpi(w, "Win Rate", fmt(finalMetrics.getWinRate() * 100) + "%",
                    delta(baseline.getWinRate(), finalMetrics.getWinRate(), 100, "%"));
            kpi(w, "PF",       fmt(finalMetrics.getProfitFactor()),
                    delta(baseline.getProfitFactor(), finalMetrics.getProfitFactor(), 1, ""));
            kpi(w, "Max DD",   fmt(Math.abs(finalMetrics.getMaxDrawdown()) * 100) + "%",
                    deltaNeg(baseline.getMaxDrawdown(), finalMetrics.getMaxDrawdown()));
            kpi(w, "Trades",   String.valueOf(finalMetrics.getTotalTrades()), "");
            w.write("</div>");

            // ── Equity Curve + Drawdown ───────────────────────────────
            if (finalResult != null) {
                PortfolioTracker pt = finalResult.getPortfolio();
                List<Long>   dates   = pt.getDates();
                List<Double> equity  = pt.getEquityCurve();
                List<Double> drawdown = pt.getDrawdownCurve();
                DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.of("Asia/Kolkata"));

                // Build JS arrays
                StringBuilder eqLabels  = new StringBuilder("[");
                StringBuilder eqVals    = new StringBuilder("[");
                StringBuilder ddVals    = new StringBuilder("[");
                for (int i = 0; i < dates.size(); i++) {
                    if (i > 0) { eqLabels.append(','); eqVals.append(','); ddVals.append(','); }
                    eqLabels.append('\'').append(dfmt.format(Instant.ofEpochMilli(dates.get(i)))).append('\'');
                    eqVals.append(String.format("%.0f", equity.get(i)));
                    ddVals.append(String.format("%.4f", drawdown.get(i) * 100));
                }
                eqLabels.append(']'); eqVals.append(']'); ddVals.append(']');

                w.write("<div class=\"section\"><h2>Equity Curve &amp; Drawdown (Optimal Config)</h2>");
                w.write("<div class=\"equity-grid\">");

                // Equity chart
                w.write("<div class=\"equity-card\">");
                w.write("<h3>Portfolio Equity (Rs)</h3>");
                w.write("<canvas id=\"eqChart\" height=\"120\"></canvas></div>");

                // Drawdown chart
                w.write("<div class=\"equity-card\">");
                w.write("<h3>Drawdown (%)</h3>");
                w.write("<canvas id=\"ddChart\" height=\"120\"></canvas></div>");

                w.write("</div></div>"); // close equity-grid + section

                // Inject chart scripts inline — will be placed after all charts in the final <script> block
                // We use a placeholder approach: write immediately into a dedicated script block here
                w.write("<script>");
                w.write("(function(){");
                w.write("var eqLabels=" + eqLabels + ";");
                w.write("var eqVals="   + eqVals   + ";");
                w.write("var ddVals="   + ddVals   + ";");
                w.write("new Chart(document.getElementById('eqChart'),{type:'line',");
                w.write("data:{labels:eqLabels,datasets:[{label:'Equity',data:eqVals,");
                w.write("borderColor:'#60a5fa',backgroundColor:'rgba(96,165,250,0.08)',");
                w.write("pointRadius:0,borderWidth:1.5,fill:true,tension:0.3}]},");
                w.write("options:{responsive:true,animation:false,");
                w.write("plugins:{legend:{display:false},");
                w.write("title:{display:true,text:'Portfolio Value (Rs)',color:'#f8fafc',font:{size:12}}},");
                w.write("scales:{x:{ticks:{color:'#94a3b8',maxTicksLimit:12,font:{size:9}},");
                w.write("grid:{color:'#1e293b'}},");
                w.write("y:{ticks:{color:'#94a3b8',callback:function(v){return (v/1e6).toFixed(2)+'M'}},");
                w.write("grid:{color:'#334155'}}}}});");
                w.write("new Chart(document.getElementById('ddChart'),{type:'line',");
                w.write("data:{labels:eqLabels,datasets:[{label:'Drawdown%',data:ddVals,");
                w.write("borderColor:'#f87171',backgroundColor:'rgba(248,113,113,0.12)',");
                w.write("pointRadius:0,borderWidth:1.5,fill:true,tension:0.3}]},");
                w.write("options:{responsive:true,animation:false,");
                w.write("plugins:{legend:{display:false},");
                w.write("title:{display:true,text:'Drawdown (%)',color:'#f8fafc',font:{size:12}}},");
                w.write("scales:{x:{ticks:{color:'#94a3b8',maxTicksLimit:12,font:{size:9}},");
                w.write("grid:{color:'#1e293b'}},");
                w.write("y:{ticks:{color:'#94a3b8',callback:function(v){return v.toFixed(1)+'%'}},");
                w.write("grid:{color:'#334155'}}}}});");
                w.write("})();");
                w.write("</script>");
            }

            // ── Baseline vs Final table ───────────────────────────────
            w.write("<div class=\"section\"><h2>Baseline vs Optimized</h2>");
            w.write("<table class=\"stats-table\"><tr><th></th><th>CAGR%</th><th>Sharpe</th>" +
                    "<th>Sortino</th><th>Win%</th><th>PF</th><th>MaxDD%</th><th>Trades</th></tr>");
            metricsRow(w, "Baseline (start config)", baseline, false);
            metricsRow(w, "Optimized (final config)", finalMetrics, true);
            w.write("</table></div>");

            // ── Final config box ──────────────────────────────────────
            w.write("<div class=\"section\"><h2>Final Optimal Configuration</h2>");
            w.write("<div class=\"config-grid\">");
            cfgItem(w, "Universe",       finalCfg.getUniverseGroupName());
            cfgItem(w, "Capital",        "₹" + fmtCr(finalCfg.getInitialCapital()));
            cfgItem(w, "Slippage",       fmt(finalCfg.getSlippage() * 100) + "%");
            cfgItem(w, "OR Minutes",     String.valueOf(finalCfg.getOpeningRangeMinutes()));
            cfgItem(w, "Stop Basis",     finalCfg.getStopBasis().name());
            cfgItem(w, "Stop ×",         fmt(finalCfg.getStopMultiplier()));
            cfgItem(w, "Target ×",       fmt(finalCfg.getTargetMultiplier()));
            cfgItem(w, "Trail Stop",     finalCfg.isTrailingStopEnabled()
                    ? finalCfg.getTrailingStopBasis().name() + " ×" + fmt(finalCfg.getTrailingStopMultiplier())
                    : "Disabled");
            cfgItem(w, "Entry Cutoff",   finalCfg.getEntryCutoffTime() != null
                    ? finalCfg.getEntryCutoffTime().toString() : "—");
            cfgItem(w, "Exit Time",      finalCfg.getExitTime() != null
                    ? finalCfg.getExitTime().toString() : "Market Close");
            cfgItem(w, "Picks",          String.valueOf(finalCfg.getPicks()));
            cfgItem(w, "Min Gap ATR",    finalCfg.getMinGapAtr() != null ? fmt(finalCfg.getMinGapAtr()) : "—");
            cfgItem(w, "Max OR IBS",     finalCfg.getMaxOrbIbs() != null ? fmt(finalCfg.getMaxOrbIbs()) : "No filter");
            cfgItem(w, "Max Re-Entries", String.valueOf(finalCfg.getMaxReEntries()));
            cfgItem(w, "ATR Scaling",    finalCfg.isAtrScaling() ? "On" : "Off");
            cfgItem(w, "Ranker",         finalCfg.getRankerType().name());
            w.write("</div></div>");

            // ── Phase-by-phase sweep charts ───────────────────────────
            w.write("<div class=\"section\"><h2>Phase-by-Phase Optimization</h2>");
            w.write("<p class=\"note\">Each chart shows all tested values for that phase. " +
                    "Bar colour: blue = positive Sharpe, red = negative / failed.</p>");
            w.write("<div class=\"phase-grid\">");

            List<String> chartIds = new ArrayList<>();
            List<String> chartScripts = new ArrayList<>();

            for (PhaseResult p : phases) {
                String cid = "chart_p" + p.phaseNum();
                chartIds.add(cid);

                // Prepare data arrays
                List<String> lbls  = new ArrayList<>();
                List<String> sharpe = new ArrayList<>();
                List<String> cagr  = new ArrayList<>();
                List<String> cols  = new ArrayList<>();
                double best_score  = p.sweepPoints().stream()
                        .filter(sp -> sp.metrics() != null)
                        .mapToDouble(sp -> score(sp.metrics()))
                        .max().orElse(0);

                for (var sp : p.sweepPoints()) {
                    lbls.add(sp.label());
                    double sh = sp.metrics() != null ? sp.metrics().getSharpe() : 0;
                    double cg = sp.metrics() != null ? sp.metrics().getCagr() * 100 : 0;
                    sharpe.add(String.format("%.2f", sh));
                    cagr.add(String.format("%.1f", cg));
                    boolean isWinner = sp.metrics() != null &&
                            Math.abs(score(sp.metrics()) - best_score) < 0.001;
                    cols.add(isWinner ? "'rgba(74,222,128,0.85)'"
                            : sh >= 0 ? "'rgba(96,165,250,0.7)'" : "'rgba(248,113,113,0.7)'");
                }

                String labStr = "[" + lbls.stream().map(l -> "'" + l.replace("'", "\\'") + "'")
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";
                String shaStr = "[" + String.join(",", sharpe) + "]";
                String colStr = "[" + String.join(",", cols) + "]";

                w.write(String.format(
                        "<div class=\"phase-card\"><h3>Phase %d: %s</h3>" +
                        "<canvas id=\"%s\" height=\"130\"></canvas></div>",
                        p.phaseNum(), esc(p.phaseName()), cid));

                chartScripts.add(String.format(
                        "new Chart(document.getElementById('%s'),{type:'bar'," +
                        "data:{labels:%s,datasets:[{label:'Sharpe',data:%s,backgroundColor:%s}]}," +
                        "options:{responsive:true,plugins:{legend:{display:false}," +
                        "title:{display:true,text:'Phase %d — %s (Sharpe)',color:'#f8fafc',font:{size:12}}}," +
                        "scales:{x:{ticks:{color:'#94a3b8',font:{size:10},maxRotation:45}}," +
                        "y:{ticks:{color:'#94a3b8'},grid:{color:'#334155'}}}}});",
                        cid, labStr, shaStr, colStr, p.phaseNum(), esc(p.phaseName())));
            }
            w.write("</div></div>"); // close phase-grid + section

            // ── Universe composition ──────────────────────────────────
            w.write("<div class=\"section\"><h2>Universe Composition (Top 50 by Score)</h2>");
            w.write("<table class=\"stats-table\"><tr>" +
                    "<th>#</th><th>Symbol</th><th>Score</th><th>Spread%</th>" +
                    "<th>ATR%</th><th>Price</th><th>Vol(K)</th><th>ADV(L)</th></tr>");
            for (int i = 0; i < Math.min(50, ranked.size()); i++) {
                var s = ranked.get(i);
                w.write(String.format(
                        "<tr><td>%d</td><td>%s</td><td>%.4f</td><td>%.2f%%</td>" +
                        "<td>%.2f%%</td><td>%.0f</td><td>%.0f</td><td>%.0f</td></tr>",
                        i+1, s.symbol(), s.compositeScore(),
                        s.avgSpreadPct(), s.avgAtrPct(),
                        s.medianPrice(), s.avgVolume() / 1000.0, s.avgTurnoverLakhs()));
            }
            w.write("</table></div>");

            // ── Chart scripts ─────────────────────────────────────────
            w.write("<script>");
            for (String sc : chartScripts) w.write(sc);
            w.write("</script>");

            // ── Footer ────────────────────────────────────────────────
            w.write("<div class=\"footer\">ORB Strategy Optimization Report &nbsp;|&nbsp; " +
                    "Capital: ₹10L &nbsp;|&nbsp; Slippage: 0.25% &nbsp;|&nbsp; " +
                    "ADV floor: ₹1 Cr &nbsp;|&nbsp; Universe: " + ranked.size() + " stocks</div>");
            w.write("</body></html>");
        }
    }

    // ── HTML helpers ──────────────────────────────────────────────────

    private static void kpi(BufferedWriter w, String label, String value, String delta)
            throws IOException {
        boolean pos = delta.startsWith("+");
        boolean neg = delta.startsWith("-") && !delta.startsWith("-0.0");
        w.write(String.format(
                "<div class=\"kpi\"><div class=\"kpi-label\">%s</div>" +
                "<div class=\"kpi-value\">%s</div>" +
                "<div class=\"kpi-delta %s\">%s</div></div>",
                label, value,
                pos ? "pos" : neg ? "neg" : "",
                delta.isEmpty() ? "—" : delta));
    }

    private static void metricsRow(BufferedWriter w, String label, RotationalMetrics m,
            boolean highlight) throws IOException {
        if (m == null) { w.write("<tr><td>" + label + "</td><td colspan=\"7\">—</td></tr>"); return; }
        w.write(String.format(
                "<tr%s><td>%s</td><td>%.1f%%</td><td>%.2f</td><td>%.2f</td>" +
                "<td>%.1f%%</td><td>%.2f</td><td>%.1f%%</td><td>%d</td></tr>",
                highlight ? " class=\"best-row\"" : "",
                label, m.getCagr() * 100, m.getSharpe(), m.getSortino(),
                m.getWinRate() * 100, m.getProfitFactor(),
                Math.abs(m.getMaxDrawdown()) * 100, m.getTotalTrades()));
    }

    private static void cfgItem(BufferedWriter w, String k, String v) throws IOException {
        w.write(String.format("<div class=\"cfg-item\"><span class=\"cfg-k\">%s</span>" +
                "<span class=\"cfg-v\">%s</span></div>", k, v));
    }

    private static String delta(double base, double opt, double scale, String unit) {
        double d = (opt - base) * scale;
        return String.format("%+.1f%s", d, unit);
    }

    private static String deltaNeg(double base, double opt) {
        // For drawdown: improvement = less negative → delta should show reduction
        double d = (Math.abs(opt) - Math.abs(base)) * 100;
        return String.format("%+.1f%%", d);
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    private static String fmtCr(double v) {
        return v >= 10_000_000 ? String.format("%.0f Cr", v / 10_000_000)
             : v >= 100_000    ? String.format("%.1f L", v / 100_000)
             : String.format("%.0f", v);
    }

    private static String esc(String s) { return s.replace("'", "\\'").replace("×", "x"); }

    private static final String CSS = """
            *{margin:0;padding:0;box-sizing:border-box}
            body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                 background:#0f172a;color:#e2e8f0;line-height:1.6;padding:20px}
            .page-header{text-align:center;padding:30px 20px;margin-bottom:20px;
                 background:linear-gradient(135deg,#1e3a5f,#1e293b);
                 border-radius:12px;border:1px solid #3b82f6}
            .page-header h1{font-size:30px;color:#f8fafc;margin-bottom:6px}
            .subtitle{color:#94a3b8;font-size:13px}
            .kpi-row{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:20px}
            .kpi{flex:1;min-width:100px;background:#1e293b;border:1px solid #334155;
                 border-radius:10px;padding:14px;text-align:center}
            .kpi-label{font-size:11px;color:#64748b;text-transform:uppercase;letter-spacing:.05em}
            .kpi-value{font-size:22px;font-weight:700;color:#f8fafc;margin:4px 0}
            .kpi-delta{font-size:12px}.kpi-delta.pos{color:#4ade80}.kpi-delta.neg{color:#f87171}
            .section{background:#1e293b;border-radius:10px;padding:22px;margin-bottom:20px;border:1px solid #334155}
            .section h2{font-size:17px;color:#f8fafc;margin-bottom:14px;
                border-bottom:1px solid #334155;padding-bottom:8px}
            .note{color:#64748b;font-size:12px;margin-bottom:12px}
            table{width:100%;border-collapse:collapse;font-size:12px}
            .stats-table td,.stats-table th{padding:7px 10px;border-bottom:1px solid #334155}
            .stats-table th{text-align:left;color:#94a3b8;font-weight:600}
            .stats-table td{text-align:right}.stats-table td:first-child{text-align:left}
            .best-row td{color:#4ade80;font-weight:600;background:#1a3a2a}
            .config-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:10px}
            .cfg-item{background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px}
            .cfg-k{display:block;font-size:10px;color:#64748b;text-transform:uppercase;margin-bottom:2px}
            .cfg-v{display:block;font-size:14px;font-weight:600;color:#60a5fa}
            .phase-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(360px,1fr));gap:16px}
            .phase-card{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:16px}
            .phase-card h3{font-size:13px;color:#94a3b8;margin-bottom:10px}
            .equity-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}
            @media(max-width:700px){.equity-grid{grid-template-columns:1fr}}
            .equity-card{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:16px}
            .equity-card h3{font-size:13px;color:#94a3b8;margin-bottom:10px}
            canvas{max-width:100%}
            .footer{text-align:center;color:#475569;font-size:12px;padding:20px}
            """;
}
