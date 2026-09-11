package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Runs statistical significance tests on the ORB rotational strategy.
 *
 * <p>Computes three complementary measures:</p>
 * <ol>
 *   <li><b>Deflated Sharpe Ratio</b> — adjusts for multiple-testing bias from
 *       parameter optimization (Bailey &amp; Lopez de Prado, 2014).</li>
 *   <li><b>Bootstrap 90% CI</b> — percentile-based confidence interval on the
 *       Sharpe ratio from 10,000 resamples.</li>
 *   <li><b>Permutation test</b> — p-value from 10,000 random shuffles of daily
 *       returns.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.StatisticalSignificanceDriver
 * </pre>
 */
public final class StatisticalSignificanceDriver {

    private static final Logger log = LoggerFactory.getLogger(StatisticalSignificanceDriver.class);

    /**
     * Estimated total number of independent parameter combinations tested
     * across all optimization phases:
     *
     * <ul>
     *   <li>Phase 1 — Stop x Target: 7 x 9 = 63</li>
     *   <li>Phase 2 — Entry cutoff: 12</li>
     *   <li>Phase 3 — Exit time: 8</li>
     *   <li>Phase 4 — Trailing stop: ~8</li>
     *   <li>Phase 5 — Gap filter: 16</li>
     *   <li>Phase 6 — Picks: 5 x 5 = 25</li>
     *   <li>Phase 7 — Re-entries: 5</li>
     *   <li>Phase 8 — OR IBS: ~10</li>
     *   <li>Phase 9 — ATR scaling: 2</li>
     *   <li>Phase 10 — Refinement: ~50</li>
     * </ul>
     *
     * <p>This is a conservative (high) estimate. Sequential optimization means
     * later phases condition on earlier winners, so the effective number of
     * independent trials is lower than the sum. We use the sum to be conservative
     * (harder to pass the DSR gate).</p>
     */
    private static final int NUM_TRIALS = 200;

    private static final int BOOTSTRAP_ITERATIONS = 10_000;
    private static final int PERMUTATION_ITERATIONS = 10_000;
    private static final double CONFIDENCE_LEVEL = 0.90;
    private static final long RANDOM_SEED = 42;

    public static void main(String[] args) throws Exception {
        log.info("==========================================================================================");
        log.info("  Statistical Significance Analysis — Rotational ORB Strategy");
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

        // ── Run the strategy with production config ──────────────────────
        RotationalBacktestConfig config = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.35)
                .maxOrbIbs(0.80)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(0.70)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.50)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(5.25)
                .entryCutoffTime(LocalTime.of(10, 30))
                .exitTime(LocalTime.of(15, 20))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(4)
                .maxReEntries(1)
                .slippage(0.001)
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();

        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
        RotationalMetrics metrics = result.getMetrics();

        log.info("Backtest complete: {} trades over {} trading days",
                metrics.getTotalTrades(), result.getPortfolio().getDailyPnl().size());
        log.info("Observed Sharpe: {}, Sortino: {}, MaxDD: {}%",
                fmt(metrics.getSharpe()), fmt(metrics.getSortino()),
                fmt(metrics.getMaxDrawdown() * 100));
        log.info("");

        // ── Extract daily returns ────────────────────────────────────────
        double[] dailyReturns = result.getPortfolio().dailyReturns();
        int T = dailyReturns.length;

        // Compute return distribution moments
        double mean = 0;
        for (double r : dailyReturns) mean += r;
        mean /= T;
        double variance = 0;
        for (double r : dailyReturns) variance += (r - mean) * (r - mean);
        variance /= T;
        double std = Math.sqrt(variance);

        double skewness = 0;
        for (double r : dailyReturns) skewness += Math.pow((r - mean) / std, 3);
        skewness /= T;

        double kurtosis = 0;
        for (double r : dailyReturns) kurtosis += Math.pow((r - mean) / std, 4);
        kurtosis /= T;

        log.info("Return distribution: T={}, mean={}, std={}, skew={}, kurtosis={} (normal=3)",
                T, fmt6(mean), fmt6(std), fmt(skewness), fmt(kurtosis));
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  1. DEFLATED SHARPE RATIO
        // ══════════════════════════════════════════════════════════════════
        log.info("==========================================================================================");
        log.info("  1. DEFLATED SHARPE RATIO (Bailey & Lopez de Prado, 2014)");
        log.info("==========================================================================================");
        log.info("");
        log.info("Number of trials (M): {} (across 9 optimization phases)", NUM_TRIALS);

        StatisticalSignificanceAnalyzer.DeflatedSharpeResult dsr =
                StatisticalSignificanceAnalyzer.deflatedSharpe(dailyReturns, NUM_TRIALS);

        log.info("");
        log.info("  Observed Sharpe:    {}", fmt(metrics.getSharpe()));
        log.info("  Expected Max SR:    {} (maximum expected from {} zero-skill trials)",
                fmt(dsr.srMax()), NUM_TRIALS);
        log.info("  Deflated Sharpe:    {} (z-score)", fmt(dsr.dsr()));
        log.info("  p-value:            {}", fmt6(dsr.pValue()));
        log.info("");
        if (dsr.pValue() < 0.01) {
            log.info("  Verdict: STRONG — p < 0.01, strategy edge is highly unlikely due to chance.");
        } else if (dsr.pValue() < 0.05) {
            log.info("  Verdict: SIGNIFICANT — p < 0.05, strategy edge is statistically significant.");
        } else if (dsr.pValue() < 0.10) {
            log.info("  Verdict: MARGINAL — p < 0.10, some evidence of edge but not conclusive.");
        } else {
            log.info("  Verdict: NOT SIGNIFICANT — p >= 0.10, observed Sharpe may be due to overfitting.");
        }
        log.info("");

        // Also compute at other trial counts for sensitivity
        log.info("  Sensitivity to number of trials:");
        log.info("  {}", String.format("  %-8s  %-10s  %-10s  %-10s", "Trials", "SR_max", "DSR", "p-value"));
        for (int trials : new int[]{50, 100, 200, 500, 1000}) {
            StatisticalSignificanceAnalyzer.DeflatedSharpeResult d =
                    StatisticalSignificanceAnalyzer.deflatedSharpe(dailyReturns, trials);
            log.info("  {}", String.format("  %-8d  %-10s  %-10s  %-10s",
                    trials, fmt(d.srMax()), fmt(d.dsr()), fmt6(d.pValue())));
        }
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  2. BOOTSTRAP CONFIDENCE INTERVALS
        // ══════════════════════════════════════════════════════════════════
        log.info("==========================================================================================");
        log.info("  2. BOOTSTRAP CONFIDENCE INTERVALS ({} iterations)", BOOTSTRAP_ITERATIONS);
        log.info("==========================================================================================");
        log.info("");

        StatisticalSignificanceAnalyzer.BootstrapResult bootstrap =
                StatisticalSignificanceAnalyzer.bootstrapSharpe(
                        dailyReturns, BOOTSTRAP_ITERATIONS, CONFIDENCE_LEVEL, RANDOM_SEED);

        log.info("  Observed Sharpe:    {}", fmt(bootstrap.observedSharpe()));
        log.info("  Median (bootstrap): {}", fmt(bootstrap.medianSharpe()));
        log.info("  {}% CI:            [{}, {}]",
                (int) (CONFIDENCE_LEVEL * 100),
                fmt(bootstrap.lower()), fmt(bootstrap.upper()));
        log.info("");
        if (bootstrap.lower() > 0) {
            log.info("  Verdict: POSITIVE — even the lower bound of the {}% CI is above zero.",
                    (int) (CONFIDENCE_LEVEL * 100));
        }
        if (bootstrap.lower() > 1.0) {
            log.info("           The lower bound exceeds 1.0, indicating a robust edge.");
        }
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  3. PERMUTATION TEST
        // ══════════════════════════════════════════════════════════════════
        log.info("==========================================================================================");
        log.info("  3. PERMUTATION TEST ({} iterations)", PERMUTATION_ITERATIONS);
        log.info("==========================================================================================");
        log.info("");

        StatisticalSignificanceAnalyzer.PermutationResult permutation =
                StatisticalSignificanceAnalyzer.permutationTest(
                        dailyReturns, PERMUTATION_ITERATIONS, RANDOM_SEED);

        log.info("  Observed Sharpe:    {}", fmt(permutation.observedSharpe()));
        log.info("  Mean null Sharpe:   {} (from shuffled returns)", fmt(permutation.meanNullSharpe()));
        log.info("  Max null Sharpe:    {}", fmt(permutation.maxNullSharpe()));
        log.info("  p-value:            {} ({}/{} permutations exceeded observed)",
                fmt6(permutation.pValue()),
                (int) (permutation.pValue() * permutation.iterations()),
                permutation.iterations());
        log.info("");
        if (permutation.pValue() < 0.01) {
            log.info("  Verdict: STRONG — p < 0.01, return distribution alone explains the Sharpe.");
        } else if (permutation.pValue() < 0.05) {
            log.info("  Verdict: SIGNIFICANT — p < 0.05, some temporal structure contributes to Sharpe.");
        } else {
            log.info("  Verdict: NOT SIGNIFICANT — p >= 0.05, Sharpe is consistent with random ordering.");
        }
        log.info("");
        log.info("  Note: For a long/short strategy whose Sharpe comes primarily from stock");
        log.info("  selection (cross-sectional alpha) rather than market timing, a high p-value");
        log.info("  is expected. The permutation test is most informative for timing-dependent");
        log.info("  strategies.");
        log.info("");

        // ══════════════════════════════════════════════════════════════════
        //  SUMMARY
        // ══════════════════════════════════════════════════════════════════
        log.info("==========================================================================================");
        log.info("  SUMMARY");
        log.info("==========================================================================================");
        log.info("");
        log.info("  {}", String.format("%-28s  %s", "Metric", "Value"));
        log.info("  {}", String.format("%-28s  %s", "---", "---"));
        log.info("  {}", String.format("%-28s  %s", "Observed Sharpe",
                fmt(metrics.getSharpe())));
        log.info("  {}", String.format("%-28s  %s (from %d trials)",
                "Expected Max SR (null)", fmt(dsr.srMax()), NUM_TRIALS));
        log.info("  {}", String.format("%-28s  %s (p=%s)",
                "Deflated Sharpe", fmt(dsr.dsr()), fmt6(dsr.pValue())));
        log.info("  {}", String.format("%-28s  [%s, %s]",
                (int) (CONFIDENCE_LEVEL * 100) + "% Bootstrap CI",
                fmt(bootstrap.lower()), fmt(bootstrap.upper())));
        log.info("  {}", String.format("%-28s  %s",
                "Permutation p-value", fmt6(permutation.pValue())));
        log.info("");

        boolean dsrPass = dsr.pValue() < 0.05;
        boolean ciPass = bootstrap.lower() > 0;

        if (dsrPass && ciPass) {
            log.info("  OVERALL: The strategy's edge appears STATISTICALLY SIGNIFICANT.");
            log.info("  The observed Sharpe survives multiple-testing correction and the");
            log.info("  confidence interval's lower bound is positive.");
        } else if (dsrPass) {
            log.info("  OVERALL: DSR is significant but bootstrap CI includes zero.");
            log.info("  The edge may be real but with high uncertainty around the Sharpe estimate.");
        } else if (ciPass) {
            log.info("  OVERALL: Bootstrap CI is positive but DSR is not significant.");
            log.info("  The Sharpe may be inflated by the number of parameter combinations tested.");
        } else {
            log.info("  OVERALL: Neither test passes. The observed Sharpe is likely overfitted.");
        }

        log.info("");
        log.info("==========================================================================================");
        log.info("  ANALYSIS COMPLETE");
        log.info("==========================================================================================");
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }

    private static String fmt6(double v) {
        return String.format("%.6f", v);
    }
}
