package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.optimizer.*;
import com.whiteowl.core.backtest.report.RotationalHtmlReport;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Parameter optimization for the SHORT-side OPPOSITE GapDirectionMode strategy
 * on the ORB Universe 250.
 *
 * <p>Optimizes Sortino ratio with robustness-based (stability) parameter selection.
 * Phases sweep the following parameters sequentially:</p>
 * <ol>
 *   <li>Stop Multiplier × Target Multiplier (2D)</li>
 *   <li>Min Gap ATR × Max Gap ATR (2D)</li>
 *   <li>Min/Max ORB RVOL</li>
 *   <li>Min/Max RS Rank</li>
 *   <li>Picks</li>
 *   <li>Ranker Type (RS, RVOL, GAP, RS_RVOL, RS_GAP, RVOL_GAP, GAP_RVOL_RS)</li>
 *   <li>Trailing Stop Multiplier</li>
 *   <li>Refinement passes</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.ShortOppositeOptimizationDriver
 * </pre>
 */
public final class ShortOppositeOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(ShortOppositeOptimizationDriver.class);

    // ── Scoring ──────────────────────────────────────────────────────────

    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 50) return Double.NEGATIVE_INFINITY;
        double s = m.getSortino();
        if (m.getMaxDrawdown() < -0.20) s -= 2.0;
        else if (m.getMaxDrawdown() < -0.12) s -= 1.0;
        return s;
    }

    private static double stabilityScore(double[] scores, int idx) {
        double self = scores[idx];
        double left = (idx > 0) ? scores[idx - 1] : 0;
        double right = (idx < scores.length - 1) ? scores[idx + 1] : 0;
        return 0.50 * self + 0.25 * left + 0.25 * right;
    }

    public static void main(String[] args) throws Exception {
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  SHORT OPPOSITE Parameter Optimization — ORB Universe 1000");
        log.info("  Sortino-Optimized with Robustness Selection");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("");

        // ── Load data ────────────────────────────────────────────────────
        String universeName = "ORB Universe - 1000";
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(universeName);
        log.info("Universe: {} ({} scrips)", universeName, scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());

        int cores = Runtime.getRuntime().availableProcessors();
        log.info("Available cores: {} (using {} for parallel runs)", cores, Math.max(1, cores - 1));
        log.info("");

        // ── Starting config ──────────────────────────────────────────────
        RotationalBacktestConfig best = RotationalBacktestConfig.builder()
                .universeGroupName(universeName)
                .startDate(LocalDate.now().minusYears(1))
                .openingRangeMinutes(5)
                .barMinutes(5)
                .side(RotationalBacktestConfig.Side.SHORT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.0)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.0)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.50)
                .entryCutoffTime(LocalTime.of(11, 0))
                .exitTime(LocalTime.of(15, 20))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .maxOrbIbs(0.80)
                .slippage(0.003)
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
        //  PHASE 1: Stop Multiplier × Target Multiplier
        // ══════════════════════════════════════════════════════════════════
        best = optimize2D(best, intradayBars, dailyBars,
                "Phase 1: Stop × Target",
                new OptimizableParameter("StopMult", "stopMultiplier", 0.4, 2.0, 0.2),
                new OptimizableParameter("TargetMult", "targetMultiplier", 1.0, 6.0, 0.5));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 2: Min Gap ATR × Max Gap ATR
        // ══════════════════════════════════════════════════════════════════
        best = optimize2D(best, intradayBars, dailyBars,
                "Phase 2: Min Gap ATR × Max Gap ATR",
                new OptimizableParameter("MinGapATR", "minGapAtr", -0.5, 1.5, 0.10),
                new OptimizableParameter("MaxGapATR", "maxGapAtr", 0.5, 5.0, 0.50));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 3: Min ORB RVOL
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig rvolBase = best;
        Double[] minRvolValues = {null, 0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 3: Min ORB RVOL", "MinOrbRvol",
                minRvolValues.length,
                i -> minRvolValues[i] != null ? fmt(minRvolValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(rvolBase);
                    b.minOrbRvol(minRvolValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 4: Max ORB RVOL
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig maxRvolBase = best;
        Double[] maxRvolValues = {null, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 4: Max ORB RVOL", "MaxOrbRvol",
                maxRvolValues.length,
                i -> maxRvolValues[i] != null ? fmt(maxRvolValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(maxRvolBase);
                    b.maxOrbRvol(maxRvolValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 5: Min RS Rank
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig rsBase = best;
        Double[] minRsValues = {null, 0.0, 10.0, 20.0, 30.0, 40.0, 50.0};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 5: Min RS Rank", "MinRsRank",
                minRsValues.length,
                i -> minRsValues[i] != null ? fmt(minRsValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(rsBase);
                    b.minRsRank(minRsValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 6: Max RS Rank
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig maxRsBase = best;
        Double[] maxRsValues = {null, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 6: Max RS Rank", "MaxRsRank",
                maxRsValues.length,
                i -> maxRsValues[i] != null ? fmt(maxRsValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(maxRsBase);
                    b.maxRsRank(maxRsValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 7: Picks
        // ══════════════════════════════════════════════════════════════════
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 7: Picks",
                new OptimizableParameter("Picks", "picks", 1, 15, 1));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 8: Ranker Type
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig rankerBase = best;
        RotationalBacktestConfig.RankerType[] rankerTypes = {
                RotationalBacktestConfig.RankerType.RS,
                RotationalBacktestConfig.RankerType.RVOL,
                RotationalBacktestConfig.RankerType.GAP,
                RotationalBacktestConfig.RankerType.RS_RVOL,
                RotationalBacktestConfig.RankerType.RS_GAP,
                RotationalBacktestConfig.RankerType.RVOL_GAP,
                RotationalBacktestConfig.RankerType.GAP_RVOL_RS,
        };
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 8: Ranker Type", "RankerType",
                rankerTypes.length,
                i -> rankerTypes[i].name(),
                i -> {
                    var b = copyToBuilder(rankerBase);
                    b.rankerType(rankerTypes[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 9: Trailing Stop
        // ══════════════════════════════════════════════════════════════════
        best = optimizeTrailingStop(best, intradayBars, dailyBars);

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 10: Entry Cutoff
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig cutoffBase = best;
        LocalTime[] cutoffTimes = {
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0)
        };
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 10: Entry Cutoff", "EntryCutoff",
                cutoffTimes.length,
                i -> cutoffTimes[i].toString(),
                i -> {
                    var b = copyToBuilder(cutoffBase);
                    b.entryCutoffTime(cutoffTimes[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 11: Max OR IBS
        // ══════════════════════════════════════════════════════════════════
        final RotationalBacktestConfig ibsBase = best;
        Double[] ibsValues = {null, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
        best = optimizeEnumParam(best, intradayBars, dailyBars,
                "Phase 11: Max OR IBS", "MaxOrbIBS",
                ibsValues.length,
                i -> ibsValues[i] != null ? fmt(ibsValues[i]) : "None",
                i -> {
                    var b = copyToBuilder(ibsBase);
                    b.maxOrbIbs(ibsValues[i]);
                    return b.build();
                });

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 12: Refinement — fine-tune Stop × Target
        // ══════════════════════════════════════════════════════════════════
        double bs = best.getStopMultiplier();
        double bt = best.getTargetMultiplier();
        best = optimize2D(best, intradayBars, dailyBars,
                "Phase 12: Refine Stop × Target",
                new OptimizableParameter("StopMult", "stopMultiplier",
                        Math.max(0.2, bs - 0.30), bs + 0.30, 0.05),
                new OptimizableParameter("TargetMult", "targetMultiplier",
                        Math.max(0.5, bt - 1.0), bt + 1.0, 0.25));

        // ══════════════════════════════════════════════════════════════════
        //  PHASE 13: Refinement — fine-tune Gap ATR
        // ══════════════════════════════════════════════════════════════════
        double bg = best.getMinGapAtr() != null ? best.getMinGapAtr() : 0.0;
        best = optimize1D(best, intradayBars, dailyBars,
                "Phase 13: Refine Min Gap ATR",
                new OptimizableParameter("MinGapATR", "minGapAtr",
                        bg - 0.20, bg + 0.20, 0.025));

        // ══════════════════════════════════════════════════════════════════
        //  FINAL RESULT
        // ══════════════════════════════════════════════════════════════════
        log.info("");
        log.info("##########################################################################################");
        log.info("  OPTIMIZATION COMPLETE — SHORT OPPOSITE on ORB Universe 1000");
        log.info("##########################################################################################");
        log.info("");

        RotationalBacktestResult finalResult = runFull(best, intradayBars, dailyBars);
        RotationalMetrics finalMetrics = finalResult.getMetrics();

        log.info("  FINAL METRICS:");
        log.info("  Sortino:       {}", fmt(finalMetrics.getSortino()));
        log.info("  Sharpe:        {}", fmt(finalMetrics.getSharpe()));
        log.info("  CAGR:          {}%", fmt(finalMetrics.getCagr() * 100));
        log.info("  Max Drawdown:  {}%", fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("  Profit Factor: {}", fmt(finalMetrics.getProfitFactor()));
        log.info("  Win Rate:      {}%", fmt(finalMetrics.getWinRate() * 100));
        log.info("  Total Trades:  {}", finalMetrics.getTotalTrades());
        log.info("  Net PnL:       {}", fmtPnl(finalMetrics.getLongPnl() + finalMetrics.getShortPnl()));
        log.info("");

        log.info("  FINAL CONFIG:");
        log.info("  Side:          {}", best.getSide());
        log.info("  GapDir:        {}", best.getGapDirectionMode());
        log.info("  Stop:          {} × {}", best.getStopBasis(), fmt(best.getStopMultiplier()));
        log.info("  Target:        {} × {}", best.getTargetBasis(), fmt(best.getTargetMultiplier()));
        log.info("  TrailingStop:  {} ({}× {})", best.isTrailingStopEnabled(),
                fmt(best.getTrailingStopMultiplier()), best.getTrailingStopBasis());
        log.info("  Entry Cutoff:  {}", best.getEntryCutoffTime());
        log.info("  Exit Time:     {}", best.getExitTime() != null ? best.getExitTime() : "MarketClose");
        log.info("  MinGapATR:     {}", best.getMinGapAtr() != null ? fmt(best.getMinGapAtr()) : "None");
        log.info("  MaxGapATR:     {}", best.getMaxGapAtr() != null ? fmt(best.getMaxGapAtr()) : "None");
        log.info("  MinOrbRvol:    {}", best.getMinOrbRvol() != null ? fmt(best.getMinOrbRvol()) : "None");
        log.info("  MaxOrbRvol:    {}", best.getMaxOrbRvol() != null ? fmt(best.getMaxOrbRvol()) : "None");
        log.info("  MinRsRank:     {}", best.getMinRsRank() != null ? fmt(best.getMinRsRank()) : "None");
        log.info("  MaxRsRank:     {}", best.getMaxRsRank() != null ? fmt(best.getMaxRsRank()) : "None");
        log.info("  Picks:         {}", best.getPicks());
        log.info("  Ranker:        {}", best.getRankerType());
        log.info("  MaxOrbIbs:     {}", best.getMaxOrbIbs() != null ? fmt(best.getMaxOrbIbs()) : "None");
        log.info("  Slippage:      {}%", fmt(best.getSlippage() * 100));
        log.info("");

        log.info("  IMPROVEMENT OVER BASELINE:");
        log.info("  {}", String.format("  Sortino: %s → %s (%+.2f)", fmt(baseline.getSortino()),
                fmt(finalMetrics.getSortino()), finalMetrics.getSortino() - baseline.getSortino()));
        log.info("  {}", String.format("  Sharpe:  %s → %s (%+.2f)", fmt(baseline.getSharpe()),
                fmt(finalMetrics.getSharpe()), finalMetrics.getSharpe() - baseline.getSharpe()));
        log.info("    CAGR:    {}% → {}%", fmt(baseline.getCagr() * 100), fmt(finalMetrics.getCagr() * 100));
        log.info("    MaxDD:   {}% → {}%", fmt(baseline.getMaxDrawdown() * 100),
                fmt(finalMetrics.getMaxDrawdown() * 100));
        log.info("");

        // ── Generate HTML Report ─────────────────────────────────────────
        Path reportPath = Path.of(System.getProperty("user.home"),
                ".whiteowl", "reports", "short_opposite_1000_report.html");
        reportPath.getParent().toFile().mkdirs();
        RotationalHtmlReport.generate(finalResult, best, reportPath);
        log.info("  HTML report: {}", reportPath.toAbsolutePath());

        // ── Export trade log CSV ─────────────────────────────────────────
        Path csvPath = reportPath.resolveSibling("short_opposite_1000_trades.csv");
        finalResult.tradeLogToCsv(csvPath);
        log.info("  Trade log:   {}", csvPath.toAbsolutePath());
        log.info("");
        log.info("##########################################################################################");
    }

    // ── 1D optimization with stability-based selection ────────────────────

    private static RotationalBacktestConfig optimize1D(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            String phaseLabel, OptimizableParameter param) throws Exception {

        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  {}", phaseLabel);
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = param.gridSize();
        log.info("  Grid: {} points for {}", total, param.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(param),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 5 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score(results.get(i).metrics());
        }
        double[] stabScores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            stabScores[i] = stabilityScore(scores, i);
        }

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

        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  {}", phaseLabel);
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        int total = p1.gridSize() * p2.gridSize();
        log.info("  Grid: {} × {} = {} points ({} × {})",
                p1.gridSize(), p2.gridSize(), total, p1.name(), p2.name());

        List<OptimizationResult> results = engine.optimize(base, List.of(p1, p2),
                intraday, daily, (completed, tot, latest) -> {
                    if (completed % 10 == 0 || completed == tot) {
                        log.info("  Progress: {}/{}", completed, tot);
                    }
                });

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

        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  {}", phaseLabel);
        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  Grid: {} values for {}", count, paramName);

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

        results.sort((a, b) -> Integer.compare((int) a[0], (int) b[0]));

        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score((RotationalMetrics) results.get(i)[2]);
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

        log.info("══════════════════════════════════════════════════════════════════════════════════════");
        log.info("  Phase 9: Trailing Stop");
        log.info("══════════════════════════════════════════════════════════════════════════════════════");

        double[] multipliers = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0};
        RotationalBacktestConfig.StopBasis[] bases = {
                RotationalBacktestConfig.StopBasis.OR_RANGE,
                RotationalBacktestConfig.StopBasis.ATR
        };

        List<Object[]> results = Collections.synchronizedList(new ArrayList<>());
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ForkJoinPool pool = new ForkJoinPool(parallelism);

        try {
            List<Runnable> tasks = new ArrayList<>();

            // Disabled
            tasks.add(() -> {
                var b = copyToBuilder(base);
                b.trailingStopEnabled(false);
                RotationalBacktestConfig cfg = b.build();
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                results.add(new Object[]{0, cfg, m, "Disabled"});
            });

            // Enabled combos
            int idx = 1;
            for (RotationalBacktestConfig.StopBasis basis : bases) {
                for (double mult : multipliers) {
                    final int ii = idx++;
                    final double mm = mult;
                    final RotationalBacktestConfig.StopBasis bb = basis;
                    tasks.add(() -> {
                        var b2 = copyToBuilder(base);
                        b2.trailingStopEnabled(true);
                        b2.trailingStopBasis(bb);
                        b2.trailingStopMultiplier(mm);
                        RotationalBacktestConfig c = b2.build();
                        RotationalMetrics m = runSingle(c, intraday, daily);
                        results.add(new Object[]{ii, c, m, String.format("%.2f× %s", mm, bb)});
                    });
                }
            }

            List<Integer> taskIndices = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) taskIndices.add(i);
            List<Runnable> taskList = tasks;
            pool.submit(() -> taskIndices.parallelStream().forEach(i -> taskList.get(i).run())).get();
        } finally {
            pool.shutdown();
        }

        results.sort((a, b) -> Integer.compare((int) a[0], (int) b[0]));

        double[] scores = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            scores[i] = score((RotationalMetrics) results.get(i)[2]);
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
            RotationalMetrics m = (RotationalMetrics) results.get(i)[2];
            if (m == null) continue;
            String marker = (i == bestIdx) ? " <-- BEST" : "";
            log.info("  {}", String.format("%-20s %8.2f %8.2f %7.1f%% %10s %7d %8.2f %8.2f%s",
                    results.get(i)[3], m.getSortino(), m.getSharpe(),
                    m.getMaxDrawdown() * 100,
                    fmtPnl(m.getLongPnl() + m.getShortPnl()),
                    m.getTotalTrades(), scores[i], stabScores[i], marker));
        }

        log.info("");
        log.info("  >> Robust best TrailingStop: {} (stability={})",
                results.get(bestIdx)[3], fmt(stabScores[bestIdx]));
        logMetrics((RotationalMetrics) results.get(bestIdx)[2]);
        return (RotationalBacktestConfig) results.get(bestIdx)[1];
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

    private static RotationalBacktestResult runFull(
            RotationalBacktestConfig config,
            Map<String, Bars> intraday, Map<String, Bars> daily) {
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        return engine.run(intraday, daily, null);
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
