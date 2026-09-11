package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.optimizer.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Walk-Forward Optimization Driver for the Rotational ORB strategy.
 *
 * <p>Performs a rolling walk-forward analysis to validate that the strategy's
 * optimized parameters generalize to unseen (out-of-sample) data. This is the
 * single most important validation step before live trading.</p>
 *
 * <h3>Walk-Forward Methodology</h3>
 * <p>The available data period (1 year / ~249 trading days) is divided into
 * rolling windows:</p>
 * <ul>
 *   <li><b>Optimization (in-sample) window:</b> 4 months — parameters are
 *       optimized via grid search on this period.</li>
 *   <li><b>Evaluation (out-of-sample) window:</b> 4 months — the best
 *       parameters from optimization are frozen and tested on this unseen data.</li>
 *   <li><b>Step size:</b> 1 month — the window rolls forward by 1 month,
 *       producing overlapping OOS windows for finer-grained analysis.</li>
 * </ul>
 *
 * <p>With 1 year of data, this produces approximately 5 walk-forward steps:</p>
 * <pre>
 *   Step 1: Optimize months 1–4,   Evaluate months 5–8
 *   Step 2: Optimize months 2–5,   Evaluate months 6–9
 *   Step 3: Optimize months 3–6,   Evaluate months 7–10
 *   Step 4: Optimize months 4–7,   Evaluate months 8–11
 *   Step 5: Optimize months 5–8,   Evaluate months 9–12
 * </pre>
 *
 * <h3>Parameters Optimized</h3>
 * <p>All 13 parameters specified in the task are optimized in each window:</p>
 * <ul>
 *   <li>minGapAtr, maxGapAtr, maxReEntries</li>
 *   <li>picks</li>
 *   <li>entryCutoffTime, exitTime</li>
 *   <li>stopBasis, stopMultiplier</li>
 *   <li>trailingStopBasis, trailingStopMultiplier</li>
 *   <li>targetBasis, targetMultiplier</li>
 * </ul>
 *
 * <h3>Key Metrics Reported</h3>
 * <ul>
 *   <li>Per-step: IS and OOS Sharpe, Sortino, MaxDD, PnL, trades, win rate, profit factor</li>
 *   <li>Aggregate OOS: combined Sharpe, MaxDD, PnL across all OOS windows</li>
 *   <li>Walk-Forward Efficiency (WFE) = OOS Sharpe / IS Sharpe</li>
 *   <li>Parameter stability across steps</li>
 * </ul>
 *
 * <p><b>Where to build:</b> As specified in the pre-live validation checklist.</p>
 */
public final class WalkForwardOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(WalkForwardOptimizationDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Walk-forward parameters
    private static final int OPTIMIZATION_MONTHS = 6;
    private static final int EVALUATION_MONTHS = 2;
    private static final int STEP_MONTHS = 1;

    // Set to true to skip optimization and use the baseline config in every step
    private static final boolean SKIP_OPTIMIZATION = false;

    // ── Holdout mode ─────────────────────────────────────────────────────
    // Phase 1: Set HOLDOUT_PHASE=1 & SKIP_OPTIMIZATION=false → runs optimiser
    //          on the first HOLDOUT_SELECTION_MONTHS of data only, to discover parameters.
    // Phase 2: Set HOLDOUT_PHASE=2 & SKIP_OPTIMIZATION=true → runs frozen params
    //          on the remaining data (pure holdout, never seen during selection).
    // Set HOLDOUT_PHASE=0 to disable holdout and use full data range as before.
    private static final int HOLDOUT_PHASE = 0;
    private static final int HOLDOUT_SELECTION_MONTHS = 6;

    /**
     * Result of a single walk-forward step.
     */
    record WalkForwardStep(
            int stepNumber,
            LocalDate optimizeStart,
            LocalDate optimizeEnd,
            LocalDate evaluateStart,
            LocalDate evaluateEnd,
            RotationalBacktestConfig optimizedConfig,
            RotationalMetrics isMetrics,
            RotationalMetrics oosMetrics,
            Map<String, String> optimizedParams
    ) {}

    // ── Scoring (same as FullParameterOptimizationDriver) ──────────────

    private static double score(RotationalMetrics m) {
        if (m == null) return Double.NEGATIVE_INFINITY;
        if (m.getTotalTrades() < 20) return Double.NEGATIVE_INFINITY;
        double s = m.getSortino();
        if (m.getMaxDrawdown() < -0.20) s -= 2.0;
        else if (m.getMaxDrawdown() < -0.12) s -= 1.0;
        return s;
    }

    // ══════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Walk-Forward Optimization — SHORT OPPOSITE on ORB Universe 1000");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info("  Optimization window:  {} months", OPTIMIZATION_MONTHS);
        log.info("  Evaluation window:    {} months", EVALUATION_MONTHS);
        log.info("  Step size:            {} months", STEP_MONTHS);
        log.info("  Skip optimization:    {}", SKIP_OPTIMIZATION);
        log.info("  Holdout phase:        {}", HOLDOUT_PHASE == 0 ? "DISABLED" : "Phase " + HOLDOUT_PHASE);
        log.info("");

        // ── Load data ────────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe - 1000");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> allIntradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> allDailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", allIntradayBars.size(), allDailyBars.size());

        // Restrict to last 1 year of data
        LocalDate dataEnd = LocalDate.MIN;
        for (Bars bars : allIntradayBars.values()) {
            if (bars.size() > 0) {
                LocalDate last = Instant.ofEpochMilli(bars.getTimestamp(bars.size() - 1))
                        .atZone(IST).toLocalDate();
                if (last.isAfter(dataEnd)) dataEnd = last;
            }
        }
        LocalDate dataStart = dataEnd.minusYears(1);
        log.info("Restricting to last 1 year: {} to {} ({} days)", dataStart, dataEnd,
                java.time.temporal.ChronoUnit.DAYS.between(dataStart, dataEnd));

        // Pre-filter bars to last 1 year
        allIntradayBars = filterBarsByDate(allIntradayBars, dataStart, dataEnd);
        allDailyBars = filterBarsByDate(allDailyBars, dataStart, dataEnd);
        log.info("After date filter: {} intraday, {} daily scrips", allIntradayBars.size(), allDailyBars.size());

        // Apply holdout restriction
        LocalDate holdoutBoundary = dataStart.plusMonths(HOLDOUT_SELECTION_MONTHS);
        if (HOLDOUT_PHASE == 1) {
            // Phase 1: restrict to first half only (parameter selection)
            dataEnd = holdoutBoundary.minusDays(1);
            log.info("HOLDOUT Phase 1: using SELECTION period only: {} to {}", dataStart, dataEnd);
        } else if (HOLDOUT_PHASE == 2) {
            // Phase 2: restrict to second half only (pure holdout validation)
            dataStart = holdoutBoundary;
            log.info("HOLDOUT Phase 2: using HOLDOUT period only: {} to {}", dataStart, dataEnd);
        }
        log.info("Effective data range: {} to {} ({} days)", dataStart, dataEnd,
                java.time.temporal.ChronoUnit.DAYS.between(dataStart, dataEnd));
        log.info("");

        int cores = Runtime.getRuntime().availableProcessors();
        log.info("Available cores: {} (using {} for parallel optimization)", cores, Math.max(1, cores - 1));
        log.info("");

        // ── Generate walk-forward windows ────────────────────────────────
        List<LocalDate[]> windows = generateWindows(dataStart, dataEnd);
        log.info("Generated {} walk-forward steps:", windows.size());
        for (int i = 0; i < windows.size(); i++) {
            LocalDate[] w = windows.get(i);
            log.info("  Step {}: Optimize {} to {}, Evaluate {} to {}",
                    i + 1, w[0], w[1], w[2], w[3]);
        }
        log.info("");

        // ── Run baseline on full data ────────────────────────────────────
        RotationalBacktestConfig baselineConfig = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe - 1000")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .side(RotationalBacktestConfig.Side.SHORT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50)
                .maxOrbIbs(0.80)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.00)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.50)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.00)
                .entryCutoffTime(LocalTime.of(11, 0))
                .exitTime(LocalTime.of(15, 20))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(0.003)  // 0.30% per trade
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();

        RotationalMetrics fullDataMetrics = runSingle(baselineConfig, allIntradayBars, allDailyBars);
        log.info("FULL-DATA BASELINE (in-sample, current config):");
        logMetrics("  ", fullDataMetrics);
        log.info("");

        // ── Execute walk-forward steps ───────────────────────────────────
        List<WalkForwardStep> steps = new ArrayList<>();
        List<RotationalTrade> allOosTrades = new ArrayList<>();
        List<double[]> allOosDailyReturns = new ArrayList<>();

        for (int i = 0; i < windows.size(); i++) {
            LocalDate[] w = windows.get(i);
            log.info("══════════════════════════════════════════════════════════════════════════════════════");
            log.info("  WALK-FORWARD STEP {}/{}", i + 1, windows.size());
            log.info("  Optimize: {} to {}   Evaluate: {} to {}", w[0], w[1], w[2], w[3]);
            log.info("══════════════════════════════════════════════════════════════════════════════════════");

            // Filter data for optimization window
            Map<String, Bars> isIntraday = filterBarsByDate(allIntradayBars, w[0], w[1]);
            Map<String, Bars> isDaily = filterBarsByDate(allDailyBars, w[0], w[1]);
            log.info("  IS data: {} intraday, {} daily scrips", isIntraday.size(), isDaily.size());

            // Filter data for evaluation window
            Map<String, Bars> oosIntraday = filterBarsByDate(allIntradayBars, w[2], w[3]);
            Map<String, Bars> oosDaily = filterBarsByDate(allDailyBars, w[2], w[3]);
            log.info("  OOS data: {} intraday, {} daily scrips", oosIntraday.size(), oosDaily.size());

            // Run optimization on IS data (or skip if using frozen params)
            RotationalBacktestConfig optimized;
            if (SKIP_OPTIMIZATION) {
                optimized = baselineConfig;
                log.info("  SKIP_OPTIMIZATION=true — using frozen baseline config");
            } else {
                optimized = runOptimization(baselineConfig, isIntraday, isDaily, i + 1);
            }

            // Evaluate on IS data (with optimized/frozen params)
            RotationalMetrics isMetrics = runSingle(optimized, isIntraday, isDaily);
            log.info("  IN-SAMPLE results ({} config):", SKIP_OPTIMIZATION ? "frozen" : "optimized");
            logMetrics("    ", isMetrics);

            // Evaluate on OOS data (frozen params)
            RotationalBacktestResult oosResult = runFullBacktest(optimized, oosIntraday, oosDaily);
            RotationalMetrics oosMetrics = oosResult.getMetrics();
            log.info("  OUT-OF-SAMPLE results (frozen params):");
            logMetrics("    ", oosMetrics);

            // Collect OOS trades for aggregate analysis
            allOosTrades.addAll(oosResult.getTradeLog());
            allOosDailyReturns.add(oosResult.getPortfolio().dailyReturns());

            // Compute WFE
            double wfe = (isMetrics.getSharpe() > 0)
                    ? oosMetrics.getSharpe() / isMetrics.getSharpe() : 0;
            log.info("  Walk-Forward Efficiency (WFE): {}", fmt(wfe));
            if (wfe >= 0.5) {
                log.info("    -> PASS (WFE >= 0.50)");
            } else {
                log.info("    -> FAIL (WFE < 0.50 — possible overfitting)");
            }

            // Record optimized parameter values
            Map<String, String> params = extractOptimizedParams(optimized);
            steps.add(new WalkForwardStep(i + 1, w[0], w[1], w[2], w[3],
                    optimized, isMetrics, oosMetrics, params));
            log.info("");
        }

        // ══════════════════════════════════════════════════════════════════
        //  AGGREGATE OOS RESULTS
        // ══════════════════════════════════════════════════════════════════
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  WALK-FORWARD ANALYSIS — AGGREGATE RESULTS");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // Summary table
        log.info(String.format("  %-6s %-11s %-11s %8s %8s %8s %8s %8s %10s %7s %7s",
                "Step", "IS Period", "OOS Period", "IS Shrp", "OOS Shrp", "IS DD%", "OOS DD%",
                "WFE", "OOS PnL", "Trades", "WinR%"));
        log.info("  " + "-".repeat(115));

        double sumOosSharpe = 0;
        double sumOosPnl = 0;
        double worstOosDd = 0;
        int totalOosTrades = 0;
        int passCount = 0;

        for (WalkForwardStep step : steps) {
            double wfe = (step.isMetrics().getSharpe() > 0)
                    ? step.oosMetrics().getSharpe() / step.isMetrics().getSharpe() : 0;
            double oosPnl = step.oosMetrics().getLongPnl() + step.oosMetrics().getShortPnl();

            log.info(String.format("  %-6d %s  %s  %8s %8s %8s %8s %8s %10s %7d %7s",
                    step.stepNumber(),
                    step.optimizeStart().format(DATE_FMT).substring(5) + "-" + step.optimizeEnd().format(DATE_FMT).substring(5),
                    step.evaluateStart().format(DATE_FMT).substring(5) + "-" + step.evaluateEnd().format(DATE_FMT).substring(5),
                    fmt(step.isMetrics().getSharpe()),
                    fmt(step.oosMetrics().getSharpe()),
                    fmt(step.isMetrics().getMaxDrawdown() * 100),
                    fmt(step.oosMetrics().getMaxDrawdown() * 100),
                    fmt(wfe),
                    fmtPnl(oosPnl),
                    step.oosMetrics().getTotalTrades(),
                    fmt(step.oosMetrics().getWinRate() * 100)));

            sumOosSharpe += step.oosMetrics().getSharpe();
            sumOosPnl += oosPnl;
            if (step.oosMetrics().getMaxDrawdown() < worstOosDd) {
                worstOosDd = step.oosMetrics().getMaxDrawdown();
            }
            totalOosTrades += step.oosMetrics().getTotalTrades();
            if (wfe >= 0.50) passCount++;
        }

        log.info("  " + "-".repeat(115));
        log.info("");

        // Aggregate OOS metrics
        double avgOosSharpe = steps.isEmpty() ? 0 : sumOosSharpe / steps.size();
        double avgWfe = 0;
        for (WalkForwardStep step : steps) {
            double wfe = (step.isMetrics().getSharpe() > 0)
                    ? step.oosMetrics().getSharpe() / step.isMetrics().getSharpe() : 0;
            avgWfe += wfe;
        }
        avgWfe = steps.isEmpty() ? 0 : avgWfe / steps.size();

        // Compute combined OOS Sharpe from concatenated daily returns
        double combinedOosSharpe = computeCombinedSharpe(allOosDailyReturns);

        log.info("  AGGREGATE OOS STATISTICS:");
        log.info("  Combined OOS Sharpe:  {}", fmt(combinedOosSharpe));
        log.info("  Average OOS Sharpe:   {}", fmt(avgOosSharpe));
        log.info("  Worst OOS MaxDD:      {}%", fmt(worstOosDd * 100));
        log.info("  Total OOS PnL:        {}", fmtPnl(sumOosPnl));
        log.info("  Total OOS Trades:     {}", totalOosTrades);
        log.info("  Average WFE:          {}", fmt(avgWfe));
        log.info("  Steps Passed (WFE>=0.5): {}/{}", passCount, steps.size());
        log.info("");

        // Compare to full-data in-sample
        log.info("  COMPARISON TO FULL-DATA IN-SAMPLE:");
        log.info("  Full-data IS Sharpe:  {} -> Combined OOS Sharpe: {} (ratio: {})",
                fmt(fullDataMetrics.getSharpe()), fmt(combinedOosSharpe),
                fmt(fullDataMetrics.getSharpe() > 0
                        ? combinedOosSharpe / fullDataMetrics.getSharpe() : 0));
        log.info("");

        // ── Parameter stability analysis ─────────────────────────────────
        log.info("  PARAMETER STABILITY ACROSS STEPS:");
        log.info(String.format("  %-22s %s", "Parameter", steps.stream()
                .map(s -> String.format("Step %-4d", s.stepNumber()))
                .collect(Collectors.joining("  "))));
        log.info("  " + "-".repeat(22 + steps.size() * 10));

        if (!steps.isEmpty()) {
            Set<String> paramNames = steps.get(0).optimizedParams().keySet();
            for (String paramName : paramNames) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  %-22s", paramName));
                for (WalkForwardStep step : steps) {
                    sb.append(String.format(" %-9s", step.optimizedParams().getOrDefault(paramName, "N/A")));
                }
                log.info(sb.toString());
            }
        }
        log.info("");

        // ── Robustness assessment ────────────────────────────────────────
        log.info("  ROBUSTNESS ASSESSMENT:");
        log.info("  " + "-".repeat(70));

        boolean oosPositive = combinedOosSharpe > 0;
        boolean oosAbove1 = combinedOosSharpe >= 1.0;
        boolean oosAbove2 = combinedOosSharpe >= 2.0;
        boolean wfeOk = avgWfe >= 0.50;
        boolean allStepsPositive = steps.stream().allMatch(s -> s.oosMetrics().getSharpe() > 0);
        boolean ddAcceptable = worstOosDd > -0.15;
        boolean majorityPass = passCount > steps.size() / 2;

        log.info("  [{}] OOS Sharpe > 0 (basic profitability)    : {}",
                oosPositive ? "PASS" : "FAIL", fmt(combinedOosSharpe));
        log.info("  [{}] OOS Sharpe >= 1.0 (meaningful edge)     : {}",
                oosAbove1 ? "PASS" : "WARN", fmt(combinedOosSharpe));
        log.info("  [{}] OOS Sharpe >= 2.0 (strong edge)         : {}",
                oosAbove2 ? "PASS" : "INFO", fmt(combinedOosSharpe));
        log.info("  [{}] Average WFE >= 0.50 (not overfitted)    : {}",
                wfeOk ? "PASS" : "FAIL", fmt(avgWfe));
        log.info("  [{}] All OOS steps profitable                : {}",
                allStepsPositive ? "PASS" : "WARN",
                allStepsPositive ? "yes" : "some steps negative");
        log.info("  [{}] Worst OOS MaxDD > -15%                  : {}%",
                ddAcceptable ? "PASS" : "WARN", fmt(worstOosDd * 100));
        log.info("  [{}] Majority of steps pass WFE >= 0.50      : {}/{}",
                majorityPass ? "PASS" : "FAIL", passCount, steps.size());
        log.info("");

        // Overall verdict
        int passScore = (oosPositive ? 1 : 0) + (oosAbove1 ? 1 : 0) + (wfeOk ? 1 : 0)
                + (allStepsPositive ? 1 : 0) + (ddAcceptable ? 1 : 0) + (majorityPass ? 1 : 0);

        String verdict;
        if (passScore >= 5) {
            verdict = "ROBUST — Strategy edge appears to generalize well to unseen data. "
                    + "Proceed to further validation (deflated Sharpe, regime analysis).";
        } else if (passScore >= 3) {
            verdict = "MODERATE — Strategy shows some OOS edge but with degradation. "
                    + "Review parameter stability and consider simplifying.";
        } else {
            verdict = "WEAK — Significant OOS degradation suggests the in-sample Sharpe "
                    + "of " + fmt(fullDataMetrics.getSharpe()) + " is likely overfitted. "
                    + "Do NOT trade live without further investigation.";
        }

        log.info("  VERDICT: {}", verdict);
        log.info("");

        // ── Export results to CSV ────────────────────────────────────────
        Path outputDir = Path.of(System.getProperty("user.home"), ".whiteowl", "output");
        Files.createDirectories(outputDir);
        Path csvPath = outputDir.resolve("walk_forward_short_opposite_1000.csv");
        exportResultsCsv(csvPath, steps, fullDataMetrics, combinedOosSharpe, avgWfe);
        log.info("  Results exported to: {}", csvPath);

        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  WALK-FORWARD ANALYSIS COMPLETE");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
    }

    // ── Window generation ─────────────────────────────────────────────────

    /**
     * Generate walk-forward windows as [optimizeStart, optimizeEnd, evalStart, evalEnd].
     * Uses a rolling approach with STEP_MONTHS step size (may differ from evaluation window).
     */
    private static List<LocalDate[]> generateWindows(LocalDate dataStart, LocalDate dataEnd) {
        List<LocalDate[]> windows = new ArrayList<>();
        LocalDate optimizeStart = dataStart;

        while (true) {
            LocalDate optimizeEnd = optimizeStart.plusMonths(OPTIMIZATION_MONTHS).minusDays(1);
            LocalDate evalStart = optimizeEnd.plusDays(1);
            LocalDate evalEnd = evalStart.plusMonths(EVALUATION_MONTHS).minusDays(1);

            // Ensure evaluation window has at least some data
            if (evalStart.isAfter(dataEnd)) break;
            if (evalEnd.isAfter(dataEnd)) {
                evalEnd = dataEnd; // truncate last window
            }
            // Need at least ~20 trading days in eval window
            if (java.time.temporal.ChronoUnit.DAYS.between(evalStart, evalEnd) < 20) break;

            windows.add(new LocalDate[]{optimizeStart, optimizeEnd, evalStart, evalEnd});
            optimizeStart = optimizeStart.plusMonths(STEP_MONTHS);
        }

        return windows;
    }

    // ── Data filtering ────────────────────────────────────────────────────

    /**
     * Filter bars to only include data within [startDate, endDate] (inclusive).
     * Creates new Bars objects containing only bars within the date range.
     */
    private static Map<String, Bars> filterBarsByDate(Map<String, Bars> allBars,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        long startEpoch = startDate.atStartOfDay(IST).toInstant().toEpochMilli();
        long endEpoch = endDate.plusDays(1).atStartOfDay(IST).toInstant().toEpochMilli();

        Map<String, Bars> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Bars> entry : allBars.entrySet()) {
            String symbol = entry.getKey();
            Bars source = entry.getValue();

            // Find start and end indices via binary search
            int from = findFirstIndex(source, startEpoch);
            int to = findLastIndex(source, endEpoch);

            if (from < 0 || to < from) continue;

            int count = to - from + 1;
            Bars sub = new Bars(symbol, source.getTimeframe(), count);
            for (int i = from; i <= to; i++) {
                sub.append(source.getTimestamp(i),
                        source.getOpen(i), source.getHigh(i),
                        source.getLow(i), source.getClose(i),
                        source.getVolume(i));
            }
            if (sub.size() > 0) {
                filtered.put(symbol, sub);
            }
        }
        return filtered;
    }

    /** Find first index where timestamp >= target. Returns -1 if none. */
    private static int findFirstIndex(Bars bars, long targetEpoch) {
        int lo = 0, hi = bars.size() - 1, result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (bars.getTimestamp(mid) >= targetEpoch) {
                result = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return result;
    }

    /** Find last index where timestamp < target. Returns -1 if none. */
    private static int findLastIndex(Bars bars, long targetEpoch) {
        int lo = 0, hi = bars.size() - 1, result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (bars.getTimestamp(mid) < targetEpoch) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    // ── Optimization ──────────────────────────────────────────────────────

    /**
     * Run multi-phase optimization on the in-sample data.
     * Optimizes all 13 parameters specified in the task.
     */
    private static RotationalBacktestConfig runOptimization(
            RotationalBacktestConfig baseConfig,
            Map<String, Bars> isIntraday,
            Map<String, Bars> isDaily,
            int stepNum) throws Exception {

        RotationalBacktestConfig best = baseConfig;

        // Phase 1: Stop Multiplier x Target Multiplier (on configurable basis)
        log.info("  [Step {}] Phase 1: Stop Multiplier x Target Multiplier...", stepNum);
        best = optimizeTwoParams(best, isIntraday, isDaily,
                new OptimizableParameter("StopMult", "stopMultiplier", 0.5, 2.0, 0.25),
                new OptimizableParameter("TargetMult", "targetMultiplier", 1.0, 4.0, 0.5));

        // Phase 2: Stop Basis + Target Basis (enumerated)
        log.info("  [Step {}] Phase 2: Stop Basis and Target Basis...", stepNum);
        best = optimizeStopAndTargetBasis(best, isIntraday, isDaily);

        // Phase 3: Entry Cutoff Time
        log.info("  [Step {}] Phase 3: Entry Cutoff Time...", stepNum);
        best = optimizeEntryCutoff(best, isIntraday, isDaily);

        // Phase 4: Exit Time
        log.info("  [Step {}] Phase 4: Exit Time...", stepNum);
        best = optimizeExitTime(best, isIntraday, isDaily);

        // Phase 5: Min Gap ATR
        log.info("  [Step {}] Phase 5: Min Gap ATR...", stepNum);
        best = optimizeOneParam(best, isIntraday, isDaily,
                new OptimizableParameter("MinGapATR", "minGapAtr", 0.0, 1.5, 0.25));

        // Phase 6: Max Gap ATR
        log.info("  [Step {}] Phase 6: Max Gap ATR...", stepNum);
        best = optimizeMaxGapAtr(best, isIntraday, isDaily);

        // Phase 7: Picks
        log.info("  [Step {}] Phase 7: Picks...", stepNum);
        best = optimizeOneParam(best, isIntraday, isDaily,
                new OptimizableParameter("Picks", "picks", 2, 8, 1));

        // Phase 8: Max Re-Entries
        log.info("  [Step {}] Phase 8: Max Re-Entries...", stepNum);
        best = optimizeOneParam(best, isIntraday, isDaily,
                new OptimizableParameter("MaxReEntries", "maxReEntries", 0, 3, 1));

        // Phase 9: Trailing Stop
        log.info("  [Step {}] Phase 9: Trailing Stop...", stepNum);
        best = optimizeTrailingStop(best, isIntraday, isDaily);

        // Phase 10: Refinement — fine-tune stop and target with smaller steps
        log.info("  [Step {}] Phase 10: Fine-tune Stop x Target...", stepNum);
        double bestStop = best.getStopMultiplier();
        double bestTarget = best.getTargetMultiplier();
        best = optimizeTwoParams(best, isIntraday, isDaily,
                new OptimizableParameter("StopMult", "stopMultiplier",
                        Math.max(0.3, bestStop - 0.3), bestStop + 0.3, 0.1),
                new OptimizableParameter("TargetMult", "targetMultiplier",
                        Math.max(0.5, bestTarget - 0.5), bestTarget + 0.5, 0.25));

        // Phase 11: Refinement — fine-tune gap filter
        log.info("  [Step {}] Phase 11: Fine-tune Gap ATR...", stepNum);
        double bestGap = best.getMinGapAtr() != null ? best.getMinGapAtr() : 0.0;
        best = optimizeOneParam(best, isIntraday, isDaily,
                new OptimizableParameter("MinGapATR", "minGapAtr",
                        Math.max(0.0, bestGap - 0.20), bestGap + 0.20, 0.10));

        log.info("  [Step {}] Optimization complete. Best config:", stepNum);
        log.info("    {}", best);

        return best;
    }

    // ── Phase helpers ─────────────────────────────────────────────────────

    private static RotationalBacktestConfig optimizeOneParam(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter param) throws Exception {

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        List<OptimizationResult> results = engine.optimize(base, List.of(param),
                intraday, daily, null);
        return pickBest(results);
    }

    private static RotationalBacktestConfig optimizeTwoParams(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily,
            OptimizableParameter p1, OptimizableParameter p2) throws Exception {

        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        List<OptimizationResult> results = engine.optimize(base, List.of(p1, p2),
                intraday, daily, null);
        return pickBest(results);
    }

    private static RotationalBacktestConfig optimizeEntryCutoff(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        LocalTime[] times = {
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0)
        };

        RotationalBacktestConfig best = base;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (LocalTime time : times) {
            var b = copyToBuilder(base);
            b.entryCutoffTime(time);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            if (s > bestScore) {
                bestScore = s;
                best = cfg;
            }
        }
        return best;
    }

    private static RotationalBacktestConfig optimizeExitTime(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        LocalTime[] times = {
                null, // market close
                LocalTime.of(15, 0), LocalTime.of(15, 10),
                LocalTime.of(15, 15), LocalTime.of(15, 20), LocalTime.of(15, 25)
        };

        RotationalBacktestConfig best = base;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (LocalTime time : times) {
            var b = copyToBuilder(base);
            b.exitTime(time);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            if (s > bestScore) {
                bestScore = s;
                best = cfg;
            }
        }
        return best;
    }

    private static RotationalBacktestConfig optimizeStopAndTargetBasis(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        RotationalBacktestConfig.StopBasis[] stopBases = RotationalBacktestConfig.StopBasis.values();
        RotationalBacktestConfig.TargetBasis[] targetBases = RotationalBacktestConfig.TargetBasis.values();

        RotationalBacktestConfig best = base;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (var sb : stopBases) {
            for (var tb : targetBases) {
                var b = copyToBuilder(base);
                b.stopBasis(sb);
                b.targetBasis(tb);
                RotationalBacktestConfig cfg = b.build();
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                double s = score(m);
                if (s > bestScore) {
                    bestScore = s;
                    best = cfg;
                }
            }
        }
        return best;
    }

    private static RotationalBacktestConfig optimizeMaxGapAtr(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        // Test: null (no max), 2.0, 3.0, 4.0, 5.0
        Double[] maxGapValues = {null, 2.0, 3.0, 4.0, 5.0};

        RotationalBacktestConfig best = base;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Double maxGap : maxGapValues) {
            var b = copyToBuilder(base);
            b.maxGapAtr(maxGap);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            if (s > bestScore) {
                bestScore = s;
                best = cfg;
            }
        }
        return best;
    }

    private static RotationalBacktestConfig optimizeTrailingStop(
            RotationalBacktestConfig base,
            Map<String, Bars> intraday, Map<String, Bars> daily) throws Exception {

        double[] multipliers = {0.75, 1.0, 1.25, 1.5, 2.0};
        RotationalBacktestConfig.StopBasis[] bases = RotationalBacktestConfig.StopBasis.values();

        RotationalBacktestConfig best = base;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Test disabled
        {
            var b = copyToBuilder(base);
            b.trailingStopEnabled(false);
            RotationalBacktestConfig cfg = b.build();
            RotationalMetrics m = runSingle(cfg, intraday, daily);
            double s = score(m);
            if (s > bestScore) { bestScore = s; best = cfg; }
        }

        // Test enabled with each combo
        for (var trailBasis : bases) {
            for (double mult : multipliers) {
                var b = copyToBuilder(base);
                b.trailingStopEnabled(true);
                b.trailingStopBasis(trailBasis);
                b.trailingStopMultiplier(mult);
                RotationalBacktestConfig cfg = b.build();
                RotationalMetrics m = runSingle(cfg, intraday, daily);
                double s = score(m);
                if (s > bestScore) { bestScore = s; best = cfg; }
            }
        }
        return best;
    }

    // ── Result picking ────────────────────────────────────────────────────

    private static RotationalBacktestConfig pickBest(List<OptimizationResult> results) {
        OptimizationResult best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (OptimizationResult r : results) {
            double s = score(r.metrics());
            if (s > bestScore) {
                bestScore = s;
                best = r;
            }
        }
        return best != null ? best.config() : results.get(0).config();
    }

    // ── Engine runners ────────────────────────────────────────────────────

    private static RotationalMetrics runSingle(
            RotationalBacktestConfig config,
            Map<String, Bars> intraday, Map<String, Bars> daily) {
        try {
            RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
            RotationalBacktestResult result = engine.run(intraday, daily, null);
            return result.getMetrics();
        } catch (Exception e) {
            log.warn("Run failed: {}", e.getMessage());
            return new RotationalMetrics(List.of(), new PortfolioTracker(config.getInitialCapital()),
                    config.getInitialCapital());
        }
    }

    private static RotationalBacktestResult runFullBacktest(
            RotationalBacktestConfig config,
            Map<String, Bars> intraday, Map<String, Bars> daily) {
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        return engine.run(intraday, daily, null);
    }

    // ── Config copy helper ────────────────────────────────────────────────

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

    // ── Param extraction for reporting ────────────────────────────────────

    private static Map<String, String> extractOptimizedParams(RotationalBacktestConfig cfg) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("minGapAtr", cfg.getMinGapAtr() != null ? fmt(cfg.getMinGapAtr()) : "null");
        params.put("maxGapAtr", cfg.getMaxGapAtr() != null ? fmt(cfg.getMaxGapAtr()) : "null");
        params.put("maxReEntries", String.valueOf(cfg.getMaxReEntries()));
        params.put("picks", String.valueOf(cfg.getPicks()));
        params.put("entryCutoff", cfg.getEntryCutoffTime().toString());
        params.put("exitTime", cfg.getExitTime() != null ? cfg.getExitTime().toString() : "MktClose");
        params.put("stopBasis", cfg.getStopBasis().name());
        params.put("stopMultiplier", fmt(cfg.getStopMultiplier()));
        params.put("trailBasis", cfg.isTrailingStopEnabled() ? cfg.getTrailingStopBasis().name() : "OFF");
        params.put("trailMultiplier", cfg.isTrailingStopEnabled() ? fmt(cfg.getTrailingStopMultiplier()) : "OFF");
        params.put("targetBasis", cfg.getTargetBasis().name());
        params.put("targetMultiplier", fmt(cfg.getTargetMultiplier()));
        return params;
    }

    // ── Aggregate OOS Sharpe ──────────────────────────────────────────────

    private static double computeCombinedSharpe(List<double[]> allDailyReturns) {
        // Concatenate all OOS daily returns
        int totalDays = 0;
        for (double[] dr : allDailyReturns) totalDays += dr.length;
        if (totalDays < 2) return 0;

        double[] combined = new double[totalDays];
        int idx = 0;
        for (double[] dr : allDailyReturns) {
            System.arraycopy(dr, 0, combined, idx, dr.length);
            idx += dr.length;
        }

        double mean = 0;
        for (double r : combined) mean += r;
        mean /= combined.length;

        double sumSq = 0;
        for (double r : combined) sumSq += (r - mean) * (r - mean);
        double std = Math.sqrt(sumSq / combined.length);

        return std > 0 ? mean / std * Math.sqrt(252) : 0;
    }

    // ── Logging helpers ───────────────────────────────────────────────────

    private static void logMetrics(String prefix, RotationalMetrics m) {
        double pnl = m.getLongPnl() + m.getShortPnl();
        log.info("{}Sharpe: {}  Sortino: {}  MaxDD: {}%  PnL: {}  Trades: {}  WinR: {}%  PF: {}",
                prefix,
                fmt(m.getSharpe()), fmt(m.getSortino()),
                fmt(m.getMaxDrawdown() * 100), fmtPnl(pnl),
                m.getTotalTrades(), fmt(m.getWinRate() * 100), fmt(m.getProfitFactor()));
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static String fmtPnl(double v) {
        return String.format("%,.0f", v);
    }

    // ── CSV Export ────────────────────────────────────────────────────────

    private static void exportResultsCsv(Path path, List<WalkForwardStep> steps,
                                          RotationalMetrics fullDataMetrics,
                                          double combinedOosSharpe, double avgWfe) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            // Header
            w.write("step,opt_start,opt_end,eval_start,eval_end,"
                    + "is_sharpe,is_sortino,is_maxdd,is_pnl,is_trades,is_winrate,is_pf,"
                    + "oos_sharpe,oos_sortino,oos_maxdd,oos_pnl,oos_trades,oos_winrate,oos_pf,"
                    + "wfe,"
                    + "minGapAtr,maxGapAtr,maxReEntries,picks,"
                    + "entryCutoff,exitTime,stopBasis,stopMult,trailBasis,trailMult,targetBasis,targetMult");
            w.newLine();

            for (WalkForwardStep step : steps) {
                double wfe = (step.isMetrics().getSharpe() > 0)
                        ? step.oosMetrics().getSharpe() / step.isMetrics().getSharpe() : 0;
                double isPnl = step.isMetrics().getLongPnl() + step.isMetrics().getShortPnl();
                double oosPnl = step.oosMetrics().getLongPnl() + step.oosMetrics().getShortPnl();

                Map<String, String> p = step.optimizedParams();

                w.write(String.join(",",
                        String.valueOf(step.stepNumber()),
                        step.optimizeStart().toString(),
                        step.optimizeEnd().toString(),
                        step.evaluateStart().toString(),
                        step.evaluateEnd().toString(),
                        fmt(step.isMetrics().getSharpe()),
                        fmt(step.isMetrics().getSortino()),
                        fmt(step.isMetrics().getMaxDrawdown() * 100),
                        String.format("%.0f", isPnl),
                        String.valueOf(step.isMetrics().getTotalTrades()),
                        fmt(step.isMetrics().getWinRate() * 100),
                        fmt(step.isMetrics().getProfitFactor()),
                        fmt(step.oosMetrics().getSharpe()),
                        fmt(step.oosMetrics().getSortino()),
                        fmt(step.oosMetrics().getMaxDrawdown() * 100),
                        String.format("%.0f", oosPnl),
                        String.valueOf(step.oosMetrics().getTotalTrades()),
                        fmt(step.oosMetrics().getWinRate() * 100),
                        fmt(step.oosMetrics().getProfitFactor()),
                        fmt(wfe),
                        p.getOrDefault("minGapAtr", ""),
                        p.getOrDefault("maxGapAtr", ""),
                        p.getOrDefault("maxReEntries", ""),
                        p.getOrDefault("picks", ""),
                        p.getOrDefault("entryCutoff", ""),
                        p.getOrDefault("exitTime", ""),
                        p.getOrDefault("stopBasis", ""),
                        p.getOrDefault("stopMultiplier", ""),
                        p.getOrDefault("trailBasis", ""),
                        p.getOrDefault("trailMultiplier", ""),
                        p.getOrDefault("targetBasis", ""),
                        p.getOrDefault("targetMultiplier", "")));
                w.newLine();
            }

            // Summary row
            w.newLine();
            w.write("# SUMMARY");
            w.newLine();
            w.write(String.format("# Full-data IS Sharpe: %s", fmt(fullDataMetrics.getSharpe())));
            w.newLine();
            w.write(String.format("# Combined OOS Sharpe: %s", fmt(combinedOosSharpe)));
            w.newLine();
            w.write(String.format("# Average WFE: %s", fmt(avgWfe)));
            w.newLine();
        }
    }
}
