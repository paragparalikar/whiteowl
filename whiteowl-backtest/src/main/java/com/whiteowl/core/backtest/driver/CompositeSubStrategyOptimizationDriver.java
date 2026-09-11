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
 * Composite sub-strategy optimization driver.
 *
 * <p>Optimizes each of the three ORB sub-strategies <em>independently</em>
 * using the robust plateau-selection method (same 11-phase approach), then
 * combines the three independently-tuned configs into a single
 * {@link CompositeBacktestEngine} run with OR-breadth capital allocation.</p>
 *
 * <h3>Sub-strategies</h3>
 * <ol>
 *   <li><b>Long-Aligned</b>  — gap-up stocks, long on OR-high break (momentum)</li>
 *   <li><b>Short-Aligned</b> — gap-down stocks, short on OR-low break (momentum)</li>
 *   <li><b>Short-Opposite</b>— gap-up stocks, short on OR-low break (mean-reversion)</li>
 * </ol>
 *
 * <h3>Capital</h3>
 * <ul>
 *   <li>₹10L, 0.25% slippage, ₹1 Cr ADV floor</li>
 *   <li>OR-breadth allocation: daily long/short split driven by breakout count ratio</li>
 * </ul>
 */
public final class CompositeSubStrategyOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(CompositeSubStrategyOptimizationDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ── Live-trading parameters ───────────────────────────────────────
    static final double CAPITAL     = 1_000_000.0; // ₹10L
    static final double SLIPPAGE    = 0.0025;       // 0.25%
    static final double ADV_FLOOR_L = 100.0;        // ₹1 Cr ADV floor

    static final String SOURCE_GROUP = "ORB Universe - Top 1000 Turnover";
    static final String LIQUID_GROUP = "ORB SmallCap 1Cr Optimized";

    // ── Phase result tracking ─────────────────────────────────────────
    record PhaseResult(int phaseNum, String phaseName,
                       List<SweepPoint> sweepPoints,
                       RotationalBacktestConfig bestConfig,
                       RotationalMetrics bestMetrics) {}

    record SweepPoint(String label, RotationalBacktestConfig config, RotationalMetrics metrics) {}

    // Per-sub-strategy phase lists — filled during optimization
    private static final List<PhaseResult> longAlignedPhases  = new ArrayList<>();
    private static final List<PhaseResult> shortAlignedPhases = new ArrayList<>();
    private static final List<PhaseResult> shortOppPhases     = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {

        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Composite Sub-Strategy Optimization — ₹10L / 0.25% slippage / ₹1 Cr ADV");
        log.info("  Each sub-strategy optimized INDEPENDENTLY, then combined with OR-Breadth allocation");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        BarsRepository barsRepo = new FileBarsRepository();

        // ── Step 1: Universe (reuse saved group, rebuild if missing) ──
        log.info("Step 1: Loading universe '{}'", LIQUID_GROUP);
        List<String> scripIds;
        try {
            scripIds = RotationalOrbDriver.loadGroupScrips(LIQUID_GROUP);
            log.info("  Loaded {} scrips from saved group", scripIds.size());
        } catch (Exception e) {
            log.info("  Group not found, rebuilding from '{}'...", SOURCE_GROUP);
            List<String> sourceIds = RotationalOrbDriver.loadGroupScrips(SOURCE_GROUP);
            Map<String, Bars> allDaily = RotationalOrbDriver.loadIntradayBars(barsRepo, sourceIds, Timeframe.DAILY);
            var filterCfg = OrbCharacteristicFilter.FilterConfig.builder()
                    .minTurnoverLakhs(ADV_FLOOR_L).minBars(100).build();
            List<OrbCharacteristicFilter.ScoredSymbol> ranked =
                    new OrbCharacteristicFilter(filterCfg).scoreAndRank(allDaily);
            Path groupsDir = Path.of(System.getProperty("whiteowl.home",
                    System.getProperty("user.home") + java.io.File.separator + ".whiteowl"),
                    "data", "groups");
            groupsDir.toFile().mkdirs();
            Files.write(groupsDir.resolve(LIQUID_GROUP + ".csv"),
                    ranked.stream().map(OrbCharacteristicFilter.ScoredSymbol::symbol).toList());
            scripIds = ranked.stream().map(OrbCharacteristicFilter.ScoredSymbol::symbol).toList();
            log.info("  Built and saved {} scrips", scripIds.size());
        }

        // ── Step 2: Load bars ─────────────────────────────────────────
        log.info("Step 2: Loading bars for {} scrips...", scripIds.size());
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars    = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("  {} intraday, {} daily loaded", intradayBars.size(), dailyBars.size());
        log.info("");

        // ══════════════════════════════════════════════════════════════
        //  PART A — Long-Aligned sub-strategy
        //  Trade: gap-up stocks only, LONG on OR-high breakout
        // ══════════════════════════════════════════════════════════════
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║  PART A: Long-Aligned Sub-Strategy (gap-up → long momentum)         ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝");
        log.info("");

        RotationalBacktestConfig laStart = RotationalBacktestConfig.builder()
                .universeGroupName(LIQUID_GROUP)
                .openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.LONG)
                .minGapAtr(0.30).maxGapAtr(1.50)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.65)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.0)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR).trailingStopMultiplier(1.25)
                .entryCutoffTime(LocalTime.of(14, 30))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(SLIPPAGE).initialCapital(CAPITAL)
                .atrScaling(false).maxReEntries(0)
                .build();

        log.info("  Baseline:");
        RotationalMetrics laBaseline = runSingle(laStart, intradayBars, dailyBars);
        logMetrics("Long-Aligned BASELINE", laBaseline);
        log.info("");

        RotationalBacktestConfig laBest = optimizeSubStrategy(
                "LA", laStart, intradayBars, dailyBars, longAlignedPhases);

        // ══════════════════════════════════════════════════════════════
        //  PART B — Short-Aligned sub-strategy
        //  Trade: gap-down stocks only, SHORT on OR-low breakout
        // ══════════════════════════════════════════════════════════════
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║  PART B: Short-Aligned Sub-Strategy (gap-down → short momentum)     ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝");
        log.info("");

        RotationalBacktestConfig saStart = RotationalBacktestConfig.builder()
                .universeGroupName(LIQUID_GROUP)
                .openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(-1.50).maxGapAtr(-0.30)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.65)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.0)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR).trailingStopMultiplier(1.25)
                .entryCutoffTime(LocalTime.of(14, 30))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(3)
                .slippage(SLIPPAGE).initialCapital(CAPITAL)
                .atrScaling(false).maxReEntries(0)
                .build();

        log.info("  Baseline:");
        RotationalMetrics saBaseline = runSingle(saStart, intradayBars, dailyBars);
        logMetrics("Short-Aligned BASELINE", saBaseline);
        log.info("");

        RotationalBacktestConfig saBest = optimizeSubStrategy(
                "SA", saStart, intradayBars, dailyBars, shortAlignedPhases);

        // ══════════════════════════════════════════════════════════════
        //  PART C — Short-Opposite sub-strategy
        //  Trade: gap-up stocks, SHORT on OR-low break (mean-reversion)
        // ══════════════════════════════════════════════════════════════
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║  PART C: Short-Opposite Sub-Strategy (gap-up → short mean-reversion)║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝");
        log.info("");

        RotationalBacktestConfig soStart = RotationalBacktestConfig.builder()
                .universeGroupName(LIQUID_GROUP)
                .openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(0.30).maxGapAtr(1.50)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.80)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.5)
                .trailingStopEnabled(false)
                .entryCutoffTime(LocalTime.of(14, 30))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(SLIPPAGE).initialCapital(CAPITAL)
                .atrScaling(false).maxReEntries(0)
                .build();

        log.info("  Baseline:");
        RotationalMetrics soBaseline = runSingle(soStart, intradayBars, dailyBars);
        logMetrics("Short-Opposite BASELINE", soBaseline);
        log.info("");

        RotationalBacktestConfig soBest = optimizeSubStrategy(
                "SO", soStart, intradayBars, dailyBars, shortOppPhases);

        // ══════════════════════════════════════════════════════════════
        //  FINAL — Combine profitable sub-strategies into CompositeBacktestEngine
        //  Any sub-strategy that ended with negative Sharpe is excluded.
        // ══════════════════════════════════════════════════════════════
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║  FINAL: Composite backtest with OR-Breadth capital allocation        ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝");
        log.info("");

        RotationalMetrics laFinal = runSingle(laBest, intradayBars, dailyBars);
        RotationalMetrics saFinal = runSingle(saBest, intradayBars, dailyBars);
        RotationalMetrics soFinal = runSingle(soBest, intradayBars, dailyBars);

        List<CompositeOrbStrategy.SubStrategy> subStrategies = new ArrayList<>();
        int totalPicks = 0;

        if (laFinal != null && laFinal.getSharpe() > 0) {
            subStrategies.add(new CompositeOrbStrategy.SubStrategy("Long-Aligned", laBest));
            totalPicks += laBest.getPicks();
            log.info("  ✓ Long-Aligned  INCLUDED: Sharpe={} CAGR={}% | {} {} | stop={}x tgt={}x",
                    String.format("%.2f", laFinal.getSharpe()),
                    String.format("%.1f", laFinal.getCagr() * 100),
                    laBest.getPicks(), laBest.getSide(),
                    laBest.getStopMultiplier(), laBest.getTargetMultiplier());
        } else {
            log.info("  ✗ Long-Aligned  EXCLUDED (Sharpe={}) — unprofitable on this universe/period",
                    laFinal != null ? String.format("%.2f", laFinal.getSharpe()) : "null");
        }

        if (saFinal != null && saFinal.getSharpe() > 0) {
            subStrategies.add(new CompositeOrbStrategy.SubStrategy("Short-Aligned", saBest));
            totalPicks += saBest.getPicks();
            log.info("  ✓ Short-Aligned INCLUDED: Sharpe={} CAGR={}% | {} {} | stop={}x tgt={}x",
                    String.format("%.2f", saFinal.getSharpe()),
                    String.format("%.1f", saFinal.getCagr() * 100),
                    saBest.getPicks(), saBest.getSide(),
                    saBest.getStopMultiplier(), saBest.getTargetMultiplier());
        } else {
            log.info("  ✗ Short-Aligned EXCLUDED (Sharpe={}) — unprofitable on this universe/period",
                    saFinal != null ? String.format("%.2f", saFinal.getSharpe()) : "null");
        }

        if (soFinal != null && soFinal.getSharpe() > 0) {
            subStrategies.add(new CompositeOrbStrategy.SubStrategy("Short-Opposite", soBest));
            totalPicks += soBest.getPicks();
            log.info("  ✓ Short-Opposite INCLUDED: Sharpe={} CAGR={}% | {} {} | stop={}x tgt={}x",
                    String.format("%.2f", soFinal.getSharpe()),
                    String.format("%.1f", soFinal.getCagr() * 100),
                    soBest.getPicks(), soBest.getSide(),
                    soBest.getStopMultiplier(), soBest.getTargetMultiplier());
        } else {
            log.info("  ✗ Short-Opposite EXCLUDED (Sharpe={}) — unprofitable on this universe/period",
                    soFinal != null ? String.format("%.2f", soFinal.getSharpe()) : "null");
        }

        log.info("");
        if (subStrategies.isEmpty()) {
            log.warn("  No profitable sub-strategies found — aborting composite run.");
            return;
        }

        CompositeOrbStrategy strategy = new CompositeOrbStrategy(
                subStrategies, totalPicks, SLIPPAGE, CAPITAL);

        // Build ranker from laBest's RankerType (all sub-strategies use RS_RVOL by default)
        RotationalBacktestEngine.EnrichedRanker compositeRanker = buildRanker(laBest.getRankerType());
        RotationalBacktestResult compositeResult =
                new CompositeBacktestEngine(strategy).run(intradayBars, dailyBars, compositeRanker);
        RotationalMetrics compositeMetrics = compositeResult.getMetrics();

        log.info("");
        logMetrics("COMPOSITE FINAL", compositeMetrics);
        log.info("");
        log.info("  Breakdown:");
        log.info("  {}", String.format("  Long PnL:  %.0f  |  Short PnL: %.0f",
                compositeMetrics.getLongPnl(), compositeMetrics.getShortPnl()));
        log.info("  {}", String.format("  Calmar:    %.2f  |  Win Rate:  %.1f%%  |  PF: %.2f",
                compositeMetrics.getCalmar(),
                compositeMetrics.getWinRate() * 100,
                compositeMetrics.getProfitFactor()));

        // ── Per-allocation-mode comparison ────────────────────────────
        log.info("");
        log.info("  (Fixed-Proportional allocation comparison...)");
        CompositeOrbStrategy fixedStrategy = new CompositeOrbStrategy(
                subStrategies, totalPicks, SLIPPAGE, CAPITAL,
                new FixedProportionalAllocator());
        RotationalBacktestEngine.EnrichedRanker fixedRanker = buildRanker(laBest.getRankerType());
        RotationalBacktestResult fixedResult = new CompositeBacktestEngine(fixedStrategy)
                .run(intradayBars, dailyBars, fixedRanker);
        RotationalMetrics fixedMetrics = fixedResult.getMetrics();
        logMetrics("COMPOSITE FIXED-PROPORTIONAL", fixedMetrics);

        // ── Single-strategy baseline for comparison ───────────────────
        log.info("");
        log.info("  (Re-running single-strategy optimal for apples-to-apples comparison...)");
        // Build a combined single config using the long-aligned params as base
        RotationalBacktestConfig singleBaseline = copyToBuilder(laBest)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ANY)
                .picks(totalPicks)
                .build();
        RotationalMetrics singleMetrics = runSingle(singleBaseline, intradayBars, dailyBars);
        logMetrics("SINGLE-STRATEGY (same picks, ANY direction)", singleMetrics);

        log.info("");
        log.info("  COMPOSITE OR-BREADTH vs SINGLE:");
        if (singleMetrics != null) {
            log.info("  {}", String.format("  Sharpe:  %.2f -> %.2f (%+.2f)",
                    singleMetrics.getSharpe(), compositeMetrics.getSharpe(),
                    compositeMetrics.getSharpe() - singleMetrics.getSharpe()));
            log.info("  {}", String.format("  CAGR:    %.1f%% -> %.1f%%",
                    singleMetrics.getCagr() * 100, compositeMetrics.getCagr() * 100));
            log.info("  {}", String.format("  MaxDD:   %.1f%% -> %.1f%%",
                    singleMetrics.getMaxDrawdown() * 100, compositeMetrics.getMaxDrawdown() * 100));
        }

        // ── HTML Report ───────────────────────────────────────────────
        log.info("");
        log.info("Generating HTML report...");
        Path reportPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports",
                "composite_substrategy_optimization.html");
        reportPath.getParent().toFile().mkdirs();
        generateHtmlReport(reportPath,
                laBest, laBaseline, longAlignedPhases,
                saBest, saBaseline, shortAlignedPhases,
                soBest, soBaseline, shortOppPhases,
                compositeResult, compositeMetrics, fixedMetrics, singleMetrics);
        log.info("Report: {}", reportPath.toAbsolutePath());
        log.info("DONE");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Sub-strategy optimization — 9 focused phases per sub-strategy
    // ══════════════════════════════════════════════════════════════════

    /**
     * Run 9 optimization phases for a single sub-strategy.
     * Phases are tailored: stop/target, entry/exit times, trailing stop,
     * gap ATR filter, picks, re-entries, IBS, fine stop/target.
     */
    private static RotationalBacktestConfig optimizeSubStrategy(
            String tag,
            RotationalBacktestConfig start,
            Map<String, Bars> intra,
            Map<String, Bars> daily,
            List<PhaseResult> phases) throws Exception {

        RotationalBacktestConfig best = start;
        int phaseOffset = phases == longAlignedPhases ? 0
                        : phases == shortAlignedPhases ? 9 : 18;

        // Phase 1: Stop × Target (coarse) — minimum stop 0.30x to avoid degenerate tight-stop configs
        log.info("════ [{}] Phase 1: Stop × Target (coarse) ════", tag);
        {
            var results = optimizeTwoParams(best, intra, daily,
                    new OptimizableParameter("StopMult",   "stopMultiplier",   0.30, 1.50, 0.30),
                    new OptimizableParameter("TargetMult", "targetMultiplier", 1.50, 6.00, 0.50));
            best = recordPhase(phaseOffset + 1, tag + " Stop×Target (coarse)", results, phases, "StopMult", "TargetMult");
        }

        // Phase 2: Entry cutoff time
        log.info("════ [{}] Phase 2: Entry Cutoff Time ════", tag);
        {
            LocalTime[] times = {
                    LocalTime.of(10, 0), LocalTime.of(10, 30),
                    LocalTime.of(11, 0), LocalTime.of(11, 30),
                    LocalTime.of(12, 0), LocalTime.of(12, 30),
                    LocalTime.of(13, 0), LocalTime.of(13, 30),
                    LocalTime.of(14, 0), LocalTime.of(14, 30),
                    LocalTime.of(15, 0), LocalTime.of(15, 15)
            };
            final var b2 = best;
            var pts = sweepSeries(best, intra, daily,
                    i -> copyToBuilder(b2).entryCutoffTime(times[i]).build(),
                    times.length, i -> times[i].toString());
            best = recordPhaseSeries(phaseOffset + 2, tag + " Entry Cutoff", pts, phases);
        }

        // Phase 3: Exit time
        log.info("════ [{}] Phase 3: Exit Time ════", tag);
        {
            LocalTime[] times = {
                    null,
                    LocalTime.of(14, 30), LocalTime.of(14, 45),
                    LocalTime.of(15, 0),  LocalTime.of(15, 10),
                    LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)
            };
            final var b3 = best;
            var pts = sweepSeries(best, intra, daily,
                    i -> copyToBuilder(b3).exitTime(times[i]).build(),
                    times.length, i -> times[i] != null ? times[i].toString() : "MarketClose");
            best = recordPhaseSeries(phaseOffset + 3, tag + " Exit Time", pts, phases);
        }

        // Phase 4: Trailing stop
        log.info("════ [{}] Phase 4: Trailing Stop ════", tag);
        {
            double[] mults = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
            RotationalBacktestConfig.StopBasis[] bases = {
                    RotationalBacktestConfig.StopBasis.OR_RANGE,
                    RotationalBacktestConfig.StopBasis.ATR
            };
            List<SweepPoint> pts = new ArrayList<>();
            var cfgOff = copyToBuilder(best).trailingStopEnabled(false).build();
            pts.add(new SweepPoint("Disabled", cfgOff, runSingle(cfgOff, intra, daily)));
            for (var basis : bases) {
                for (double m : mults) {
                    var cfg = copyToBuilder(best).trailingStopEnabled(true)
                            .trailingStopBasis(basis).trailingStopMultiplier(m).build();
                    pts.add(new SweepPoint(basis.name() + "x" + m, cfg, runSingle(cfg, intra, daily)));
                }
            }
            best = recordPhaseSeries(phaseOffset + 4, tag + " Trailing Stop", pts, phases);
        }

        // Phase 5: Picks count
        log.info("════ [{}] Phase 5: Picks Count ════", tag);
        {
            List<SweepPoint> pts = new ArrayList<>();
            for (int n = 1; n <= 8; n++) {
                var cfg = copyToBuilder(best).picks(n).build();
                pts.add(new SweepPoint(String.valueOf(n), cfg, runSingle(cfg, intra, daily)));
            }
            best = recordPhaseSeries(phaseOffset + 5, tag + " Picks", pts, phases);
        }

        // Phase 6: Max re-entries
        log.info("════ [{}] Phase 6: Max Re-Entries ════", tag);
        {
            var results = optimizeOneParam(best, intra, daily,
                    new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));
            best = recordPhase(phaseOffset + 6, tag + " Re-Entries", results, phases, "MaxReEntries");
        }

        // Phase 7: Max OR IBS filter
        log.info("════ [{}] Phase 7: Max OR IBS ════", tag);
        {
            List<SweepPoint> pts = new ArrayList<>();
            var cfgNone = copyToBuilder(best).maxOrbIbs(null).build();
            pts.add(new SweepPoint("No filter", cfgNone, runSingle(cfgNone, intra, daily)));
            for (double ibs = 0.3; ibs <= 0.95; ibs += 0.10) {
                double v = Math.round(ibs * 10) / 10.0;
                var cfg = copyToBuilder(best).maxOrbIbs(v).build();
                pts.add(new SweepPoint(String.format("%.1f", v), cfg, runSingle(cfg, intra, daily)));
            }
            best = recordPhaseSeries(phaseOffset + 7, tag + " Max OR IBS", pts, phases);
        }

        // Phase 8: Stop × Target (fine refinement)
        log.info("════ [{}] Phase 8: Stop × Target (fine) ════", tag);
        {
            double bs = best.getStopMultiplier(), bt = best.getTargetMultiplier();
            // Minimum stop of 0.20x to prevent degenerate near-zero-stop configs
            var results = optimizeTwoParams(best, intra, daily,
                    new OptimizableParameter("StopMult",   "stopMultiplier",
                            Math.max(0.20, bs - 0.25), bs + 0.25, 0.05),
                    new OptimizableParameter("TargetMult", "targetMultiplier",
                            Math.max(0.50, bt - 1.00), bt + 1.00, 0.25));
            best = recordPhase(phaseOffset + 8, tag + " Stop×Target (fine)", results, phases, "StopMult", "TargetMult");
        }

        // Phase 9: Min gap ATR (fine)
        log.info("════ [{}] Phase 9: Min Gap ATR (fine) ════", tag);
        {
            boolean isOpposite = start.getGapDirectionMode()
                    == RotationalBacktestConfig.GapDirectionMode.OPPOSITE;
            boolean isShortAligned = start.getSide() == RotationalBacktestConfig.Side.SHORT && !isOpposite;
            List<SweepPoint> pts = new ArrayList<>();
            double[] vals = {0.0, 0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90, 1.00, 1.20, 1.50};
            for (double v : vals) {
                double gapVal = isShortAligned ? -v : v;
                var cfg = copyToBuilder(best).minGapAtr(gapVal).build();
                pts.add(new SweepPoint(String.format("%.2f", v), cfg, runSingle(cfg, intra, daily)));
            }
            best = recordPhaseSeries(phaseOffset + 9, tag + " Min Gap ATR", pts, phases);
        }

        // Safety net: if final best is still negative Sharpe, fall back to the best
        // phase result seen across ALL phases (avoids optimizer making things worse).
        RotationalMetrics finalMetrics = runSingle(best, intra, daily);
        if (finalMetrics == null || finalMetrics.getSharpe() < 0) {
            RotationalBacktestConfig globalBest = best;
            double globalBestScore = finalMetrics != null ? score(finalMetrics) : Double.NEGATIVE_INFINITY;
            for (PhaseResult pr : phases) {
                if (pr.bestMetrics() != null && score(pr.bestMetrics()) > globalBestScore) {
                    globalBestScore = score(pr.bestMetrics());
                    globalBest = pr.bestConfig();
                }
            }
            if (globalBest != best) {
                RotationalMetrics gm = runSingle(globalBest, intra, daily);
                log.info("  [{}] Final phase negative (Sharpe={}), reverting to global best (Sharpe={})",
                        tag,
                        finalMetrics != null ? String.format("%.2f", finalMetrics.getSharpe()) : "null",
                        gm != null ? String.format("%.2f", gm.getSharpe()) : "null");
                best = globalBest;
                finalMetrics = gm;
            }
        }

        log.info("  ✓ [{}] optimization complete. Best config: {} {}  stop={}  tgt={}",
                tag, best.getPicks(), best.getSide(),
                best.getStopMultiplier(), best.getTargetMultiplier());
        if (finalMetrics != null) {
            logMetrics("  [" + tag + "] Final", finalMetrics);
        }
        log.info("");
        return best;
    }

    // ══════════════════════════════════════════════════════════════════
    //  Phase helpers
    // ══════════════════════════════════════════════════════════════════

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
        log.info("  Grid: {} x {} = {} ({} x {})", p1.gridSize(), p2.gridSize(), total, p1.name(), p2.name());
        return new ParameterOptimizationEngine().optimize(base, List.of(p1, p2), intra, daily,
                (done, tot, latest) -> { if (done % 10 == 0 || done == tot) log.info("  {}/{}", done, tot); });
    }

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

    private static RotationalBacktestConfig recordPhase(int num, String name,
            List<OptimizationResult> results, List<PhaseResult> phases, String... paramNames) {
        List<SweepPoint> pts = new ArrayList<>();
        for (var r : results) {
            String lbl = paramNames.length == 2
                    ? String.format("%.2f/%.2f", r.paramValues()[0], r.paramValues()[1])
                    : String.format("%.2f", r.paramValues()[0]);
            pts.add(new SweepPoint(lbl, r.config(), r.metrics()));
        }
        return recordPhaseSeries(num, name, pts, phases);
    }

    /**
     * Plateau-robustness selection: prefer the stable centre of a performance
     * plateau over an isolated peak. Same algorithm as SmallCapOptimizationDriver.
     */
    private static RotationalBacktestConfig recordPhaseSeries(int num, String name,
            List<SweepPoint> pts, List<PhaseResult> phases) {

        List<SweepPoint> valid = pts.stream()
                .filter(p -> p.metrics() != null && score(p.metrics()) > Double.NEGATIVE_INFINITY)
                .toList();
        if (valid.isEmpty()) {
            SweepPoint fallback = pts.get(0);
            phases.add(new PhaseResult(num, name, pts, fallback.config(), fallback.metrics()));
            log.info("  Phase {} -- no valid runs", num);
            log.info("");
            return fallback.config();
        }

        double maxScore  = valid.stream().mapToDouble(p -> score(p.metrics())).max().orElse(0);
        double threshold = maxScore * 0.90;

        List<SweepPoint> allList = new ArrayList<>(pts);
        SweepPoint robustPt      = null;
        int bestNeighbours       = -1;

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
                    (neighbours == bestNeighbours && robustPt != null
                            && score(candidate.metrics()) > score(robustPt.metrics()))) {
                bestNeighbours = neighbours;
                robustPt = candidate;
            }
        }
        if (robustPt == null) robustPt = valid.get(0);

        // Skip isolated raw peak
        boolean isIsolatedPeak = Math.abs(score(robustPt.metrics()) - maxScore) < 0.001
                && bestNeighbours == 0;
        if (isIsolatedPeak && valid.size() > 1) {
            final SweepPoint peak = robustPt;
            SweepPoint alt = valid.stream()
                    .filter(p -> p != peak && score(p.metrics()) >= threshold)
                    .max(Comparator.comparingDouble(p -> score(p.metrics())))
                    .orElse(null);
            if (alt != null) {
                log.info("  Phase {} -- isolated peak [{}], chose robust alt [{}]",
                        num, peak.label(), alt.label());
                robustPt = alt;
            }
        }

        phases.add(new PhaseResult(num, name, pts, robustPt.config(), robustPt.metrics()));
        log.info("  {}", String.format("  Phase %d plateau: [%s]  neighbours=%d  score=%.2f  peak=%.2f",
                num, robustPt.label(), bestNeighbours, score(robustPt.metrics()), maxScore));
        logMetrics("Phase " + num + " robust [" + robustPt.label() + "]", robustPt.metrics());
        log.info("");
        return robustPt.config();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Score / run helpers
    // ══════════════════════════════════════════════════════════════════

    private static RotationalBacktestEngine.EnrichedRanker buildRanker(
            RotationalBacktestConfig.RankerType type) {
        return switch (type) {
            case GAP_RVOL_RS -> new GapRvolRsRanker();
            default          -> new ConfigurableRanker(false, true, true); // RS_RVOL default
        };
    }

    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 50) return Double.NEGATIVE_INFINITY;
        if (m.getWinRate() < 0.25) return Double.NEGATIVE_INFINITY;  // degenerate: stop too tight
        if (m.getProfitFactor() < 0.30) return Double.NEGATIVE_INFINITY; // near-zero PF
        double s = m.getSharpe() + 0.1 * m.getSortino();
        if (m.getMaxDrawdown() < -0.20) s -= 1.0;
        if (m.getMaxDrawdown() < -0.15) s -= 0.5;
        return s;
    }

    private static RotationalMetrics runSingle(RotationalBacktestConfig cfg,
            Map<String, Bars> intra, Map<String, Bars> daily) {
        try {
            return new RotationalBacktestEngine(cfg).run(intra, daily, null).getMetrics();
        } catch (Exception e) {
            log.warn("Run failed: {}", e.getMessage());
            return null;
        }
    }

    private static void logMetrics(String label, RotationalMetrics m) {
        if (m == null) { log.info("  {}: null", label); return; }
        log.info("  {}", String.format(
                "  %s: CAGR=%.1f%%  Sharpe=%.2f  Sortino=%.2f  WinRate=%.1f%%  PF=%.2f  MaxDD=%.1f%%  Trades=%d",
                label, m.getCagr() * 100, m.getSharpe(), m.getSortino(),
                m.getWinRate() * 100, m.getProfitFactor(),
                Math.abs(m.getMaxDrawdown()) * 100, m.getTotalTrades()));
    }

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
            RotationalBacktestConfig laCfg, RotationalMetrics laBase, List<PhaseResult> laPhases,
            RotationalBacktestConfig saCfg, RotationalMetrics saBase, List<PhaseResult> saPhases,
            RotationalBacktestConfig soCfg, RotationalMetrics soBase, List<PhaseResult> soPhases,
            RotationalBacktestResult compositeResult, RotationalMetrics compositeMetrics,
            RotationalMetrics fixedMetrics, RotationalMetrics singleMetrics) throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(out)) {

            w.write("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
            w.write("<title>Composite Sub-Strategy Optimization</title>");
            w.write("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>");
            w.write("<style>" + CSS + "</style></head><body>");

            // Header
            w.write("<div class=\"page-header\">");
            w.write("<h1>Composite Sub-Strategy Optimization</h1>");
            w.write("<p class=\"subtitle\">Each sub-strategy independently optimized &nbsp;|&nbsp; ");
            w.write("OR-Breadth capital allocation &nbsp;|&nbsp; ");
            w.write("Capital: ₹10L &nbsp;|&nbsp; Slippage: 0.25% &nbsp;|&nbsp; ADV floor: ₹1 Cr &nbsp;|&nbsp; ");
            w.write("Generated: " + LocalDateTime.now(IST).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " IST</p></div>");

            // ── KPI: Composite OR-Breadth ─────────────────────────────
            w.write("<div class=\"section\"><h2>Composite Strategy — Final Performance (OR-Breadth)</h2>");
            w.write("<div class=\"kpi-row\">");
            kpi(w, "CAGR",     fmt(compositeMetrics.getCagr() * 100) + "%",
                    singleMetrics != null ? delta(singleMetrics.getCagr(), compositeMetrics.getCagr(), 100, "%") : "");
            kpi(w, "Sharpe",   fmt(compositeMetrics.getSharpe()),
                    singleMetrics != null ? delta(singleMetrics.getSharpe(), compositeMetrics.getSharpe(), 1, "") : "");
            kpi(w, "Sortino",  fmt(compositeMetrics.getSortino()),
                    singleMetrics != null ? delta(singleMetrics.getSortino(), compositeMetrics.getSortino(), 1, "") : "");
            kpi(w, "Win Rate", fmt(compositeMetrics.getWinRate() * 100) + "%", "");
            kpi(w, "PF",       fmt(compositeMetrics.getProfitFactor()), "");
            kpi(w, "Max DD",   fmt(Math.abs(compositeMetrics.getMaxDrawdown()) * 100) + "%",
                    singleMetrics != null ? deltaNeg(singleMetrics.getMaxDrawdown(), compositeMetrics.getMaxDrawdown()) : "");
            kpi(w, "Trades",   String.valueOf(compositeMetrics.getTotalTrades()), "");
            w.write("</div>");

            // Comparison table: single → fixed → OR-breadth
            w.write("<table class=\"stats-table\"><tr><th></th><th>CAGR%</th><th>Sharpe</th>" +
                    "<th>Sortino</th><th>Win%</th><th>PF</th><th>MaxDD%</th><th>Trades</th></tr>");
            if (singleMetrics != null)
                metricsRow(w, "Single-strategy baseline (same picks, ANY direction)", singleMetrics, false);
            if (fixedMetrics != null)
                metricsRow(w, "Composite + Fixed-Proportional allocation", fixedMetrics, false);
            metricsRow(w, "Composite + OR-Breadth allocation (dynamic tilt)", compositeMetrics, true);
            w.write("</table></div>");

            // ── Equity Curve + Drawdown ───────────────────────────────
            PortfolioTracker pt = compositeResult.getPortfolio();
            List<Long>   dates    = pt.getDates();
            List<Double> equity   = pt.getEquityCurve();
            List<Double> drawdown = pt.getDrawdownCurve();
            DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(IST);

            StringBuilder eqLabels = new StringBuilder("[");
            StringBuilder eqVals   = new StringBuilder("[");
            StringBuilder ddVals   = new StringBuilder("[");
            for (int i = 0; i < dates.size(); i++) {
                if (i > 0) { eqLabels.append(','); eqVals.append(','); ddVals.append(','); }
                eqLabels.append('\'').append(dfmt.format(Instant.ofEpochMilli(dates.get(i)))).append('\'');
                eqVals.append(String.format("%.0f", equity.get(i)));
                ddVals.append(String.format("%.4f", drawdown.get(i) * 100));
            }
            eqLabels.append(']'); eqVals.append(']'); ddVals.append(']');

            w.write("<div class=\"section\"><h2>Composite Equity Curve &amp; Drawdown</h2>");
            w.write("<div class=\"equity-grid\">");
            w.write("<div class=\"equity-card\"><h3>Portfolio Equity (₹)</h3>");
            w.write("<canvas id=\"eqChart\" height=\"120\"></canvas></div>");
            w.write("<div class=\"equity-card\"><h3>Drawdown (%)</h3>");
            w.write("<canvas id=\"ddChart\" height=\"120\"></canvas></div>");
            w.write("</div></div>");

            w.write("<script>(function(){");
            w.write("var L=" + eqLabels + ",E=" + eqVals + ",D=" + ddVals + ";");
            w.write("new Chart(document.getElementById('eqChart'),{type:'line',data:{labels:L,datasets:[{label:'Equity',data:E,borderColor:'#60a5fa',backgroundColor:'rgba(96,165,250,0.08)',pointRadius:0,borderWidth:1.5,fill:true,tension:0.3}]},options:{responsive:true,animation:false,plugins:{legend:{display:false}},scales:{x:{ticks:{color:'#94a3b8',maxTicksLimit:12,font:{size:9}},grid:{color:'#1e293b'}},y:{ticks:{color:'#94a3b8',callback:function(v){return '₹'+(v/1e5).toFixed(0)+'L'}},grid:{color:'#334155'}}}}});");
            w.write("new Chart(document.getElementById('ddChart'),{type:'line',data:{labels:L,datasets:[{label:'DD%',data:D,borderColor:'#f87171',backgroundColor:'rgba(248,113,113,0.12)',pointRadius:0,borderWidth:1.5,fill:true,tension:0.3}]},options:{responsive:true,animation:false,plugins:{legend:{display:false}},scales:{x:{ticks:{color:'#94a3b8',maxTicksLimit:12,font:{size:9}},grid:{color:'#1e293b'}},y:{ticks:{color:'#94a3b8',callback:function(v){return v.toFixed(1)+'%'}},grid:{color:'#334155'}}}}});");
            w.write("})();</script>");

            // ── Sub-strategy configs ──────────────────────────────────
            w.write("<div class=\"section\"><h2>Optimized Sub-Strategy Configurations</h2>");
            w.write("<div class=\"substrat-grid\">");
            subStratCard(w, "Long-Aligned", "gap-up → momentum long", "#3b82f6", laCfg, laBase);
            subStratCard(w, "Short-Aligned", "gap-down → momentum short", "#a855f7", saCfg, saBase);
            subStratCard(w, "Short-Opposite", "gap-up → mean-reversion short", "#f59e0b", soCfg, soBase);
            w.write("</div></div>");

            // ── Phase charts: one section per sub-strategy ────────────
            List<String> chartScripts = new ArrayList<>();
            renderSubStratPhases(w, "Long-Aligned", "#3b82f6", laPhases, chartScripts);
            renderSubStratPhases(w, "Short-Aligned", "#a855f7", saPhases, chartScripts);
            renderSubStratPhases(w, "Short-Opposite", "#f59e0b", soPhases, chartScripts);

            // Chart scripts
            w.write("<script>");
            for (String sc : chartScripts) w.write(sc);
            w.write("</script>");

            w.write("<div class=\"footer\">Composite Sub-Strategy Optimization &nbsp;|&nbsp; " +
                    "₹10L &nbsp;|&nbsp; 0.25% slippage &nbsp;|&nbsp; ₹1 Cr ADV floor</div>");
            w.write("</body></html>");
        }
    }

    // ── Sub-strategy card ─────────────────────────────────────────────

    private static void subStratCard(BufferedWriter w, String name, String desc,
            String colour, RotationalBacktestConfig cfg, RotationalMetrics base) throws IOException {
        w.write(String.format("<div class=\"substrat-card\" style=\"border-color:%s\">", colour));
        w.write(String.format("<h3 style=\"color:%s\">%s</h3><p class=\"desc\">%s</p>", colour, name, desc));
        w.write("<table class=\"mini-table\">");
        miniRow(w, "Stop",          fmt(cfg.getStopMultiplier()) + "x OR_RANGE");
        miniRow(w, "Target",        fmt(cfg.getTargetMultiplier()) + "x stop");
        miniRow(w, "Trail Stop",    cfg.isTrailingStopEnabled()
                ? cfg.getTrailingStopBasis().name() + " x" + fmt(cfg.getTrailingStopMultiplier())
                : "Disabled");
        miniRow(w, "Entry Cutoff",  cfg.getEntryCutoffTime() != null ? cfg.getEntryCutoffTime().toString() : "∞");
        miniRow(w, "Exit Time",     cfg.getExitTime() != null ? cfg.getExitTime().toString() : "Market Close");
        miniRow(w, "Picks",         cfg.getPicks() + " " + cfg.getSide().name().toLowerCase());
        miniRow(w, "Max OR IBS",    cfg.getMaxOrbIbs() != null ? fmt(cfg.getMaxOrbIbs()) : "No filter");
        miniRow(w, "Re-entries",    String.valueOf(cfg.getMaxReEntries()));
        w.write("</table>");
        if (base != null) {
            w.write(String.format("<p class=\"baseline-note\">Baseline: CAGR %.1f%% | Sharpe %.2f | DD %.1f%%</p>",
                    base.getCagr() * 100, base.getSharpe(), Math.abs(base.getMaxDrawdown()) * 100));
        }
        w.write("</div>");
    }

    private static void miniRow(BufferedWriter w, String k, String v) throws IOException {
        w.write(String.format("<tr><td class=\"mk\">%s</td><td class=\"mv\">%s</td></tr>", k, v));
    }

    // ── Phase chart section ───────────────────────────────────────────

    private static void renderSubStratPhases(BufferedWriter w, String subName, String colour,
            List<PhaseResult> phases, List<String> scripts) throws IOException {
        if (phases.isEmpty()) return;
        w.write(String.format("<div class=\"section\"><h2>%s — Phase-by-Phase Optimization</h2>", subName));
        w.write(String.format("<p class=\"note\" style=\"color:%s\">%s &nbsp;|&nbsp; ", colour, subName) +
                "Green bar = robust winner selected &nbsp;|&nbsp; Blue = positive Sharpe &nbsp;|&nbsp; Red = negative/failed</p>");
        w.write("<div class=\"phase-grid\">");
        for (PhaseResult p : phases) {
            String cid = "chart_" + subName.replace("-","").replace(" ","") + "_p" + p.phaseNum();
            double bestScore = p.sweepPoints().stream()
                    .filter(sp -> sp.metrics() != null)
                    .mapToDouble(sp -> score(sp.metrics()))
                    .max().orElse(0);

            List<String> lbls = new ArrayList<>(), sharpes = new ArrayList<>(), cols = new ArrayList<>();
            for (var sp : p.sweepPoints()) {
                lbls.add(sp.label());
                double sh = sp.metrics() != null ? sp.metrics().getSharpe() : 0;
                sharpes.add(String.format("%.2f", sh));
                boolean winner = sp.metrics() != null && Math.abs(score(sp.metrics()) - bestScore) < 0.001;
                cols.add(winner ? "'rgba(74,222,128,0.85)'"
                        : sh >= 0 ? "'rgba(96,165,250,0.7)'" : "'rgba(248,113,113,0.7)'");
                // Override winner colour with sub-strategy colour for picked point
                if (winner) cols.set(cols.size()-1,
                        String.format("'%s'", colour.replace("#", "rgba(") + ",0.9)"));
            }
            String labStr = "[" + lbls.stream().map(l -> "'" + l.replace("'","\\'") + "'")
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";
            String shaStr = "[" + String.join(",", sharpes) + "]";
            String colStr = "[" + String.join(",", cols) + "]";

            w.write(String.format(
                    "<div class=\"phase-card\"><h3>Phase %d: %s</h3><canvas id=\"%s\" height=\"130\"></canvas></div>",
                    p.phaseNum(), esc(p.phaseName()), cid));

            scripts.add(String.format(
                    "new Chart(document.getElementById('%s'),{type:'bar'," +
                    "data:{labels:%s,datasets:[{label:'Sharpe',data:%s,backgroundColor:%s}]}," +
                    "options:{responsive:true,plugins:{legend:{display:false}," +
                    "title:{display:true,text:'%s',color:'#f8fafc',font:{size:11}}}," +
                    "scales:{x:{ticks:{color:'#94a3b8',font:{size:9},maxRotation:45}}," +
                    "y:{ticks:{color:'#94a3b8'},grid:{color:'#334155'}}}}});",
                    cid, labStr, shaStr, colStr, esc(p.phaseName())));
        }
        w.write("</div></div>");
    }

    // ── HTML helpers ──────────────────────────────────────────────────

    private static void kpi(BufferedWriter w, String label, String value, String delta) throws IOException {
        boolean pos = delta.startsWith("+");
        boolean neg = delta.startsWith("-") && !delta.startsWith("-0.0");
        w.write(String.format(
                "<div class=\"kpi\"><div class=\"kpi-label\">%s</div>" +
                "<div class=\"kpi-value\">%s</div>" +
                "<div class=\"kpi-delta %s\">%s</div></div>",
                label, value, pos ? "pos" : neg ? "neg" : "",
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

    private static String delta(double base, double opt, double scale, String unit) {
        return String.format("%+.1f%s", (opt - base) * scale, unit);
    }
    private static String deltaNeg(double base, double opt) {
        return String.format("%+.1f%%", (Math.abs(opt) - Math.abs(base)) * 100);
    }
    private static String fmt(double v)  { return String.format("%.2f", v); }
    private static String esc(String s)  { return s.replace("'", "\\'"); }

    private static final String CSS = """
            *{margin:0;padding:0;box-sizing:border-box}
            body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                 background:#0f172a;color:#e2e8f0;line-height:1.6;padding:20px}
            .page-header{text-align:center;padding:30px 20px;margin-bottom:20px;
                 background:linear-gradient(135deg,#1e3a5f,#1e293b);
                 border-radius:12px;border:1px solid #3b82f6}
            .page-header h1{font-size:28px;color:#f8fafc;margin-bottom:6px}
            .subtitle{color:#94a3b8;font-size:13px}
            .kpi-row{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:16px}
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
            .substrat-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}
            @media(max-width:900px){.substrat-grid{grid-template-columns:1fr}}
            .substrat-card{background:#0f172a;border:2px solid #334155;border-radius:10px;padding:16px}
            .substrat-card h3{font-size:15px;font-weight:700;margin-bottom:4px}
            .desc{font-size:11px;color:#64748b;margin-bottom:10px}
            .mini-table{width:100%;font-size:11px;margin-bottom:8px}
            .mini-table tr{border-bottom:1px solid #1e293b}
            .mk{color:#64748b;padding:3px 0;width:45%}.mv{color:#e2e8f0;font-weight:600;text-align:right}
            .baseline-note{font-size:10px;color:#475569;margin-top:6px}
            .phase-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:14px}
            .phase-card{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:14px}
            .phase-card h3{font-size:12px;color:#94a3b8;margin-bottom:8px}
            .equity-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}
            @media(max-width:700px){.equity-grid{grid-template-columns:1fr}}
            .equity-card{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:16px}
            .equity-card h3{font-size:13px;color:#94a3b8;margin-bottom:10px}
            canvas{max-width:100%}
            .footer{text-align:center;color:#475569;font-size:12px;padding:20px}
            """;
}
