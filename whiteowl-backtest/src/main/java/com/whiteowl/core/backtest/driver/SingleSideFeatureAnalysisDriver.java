package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.feature.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

/**
 * Feature analysis driver for single-side ORB strategies.
 *
 * <p>Runs each of the 4 directional scenarios (long-aligned, long-opposite,
 * short-aligned, short-opposite) with maximally liberal filters — no gap, RVOL,
 * RS, or IBS filtering, picks set to universe size — to capture ALL possible
 * trades for each scenario.</p>
 *
 * <p>Then computes per-trade features (GAP_ATR, OR_RVOL, RS_RANK, OR_RANGE_ATR,
 * OR_IBS) and runs {@link BucketAnalyzer} sweep analysis on each feature to
 * determine sensible min/max/step optimization ranges.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.SingleSideFeatureAnalysisDriver
 * </pre>
 */
public final class SingleSideFeatureAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(SingleSideFeatureAnalysisDriver.class);

    private static final String UNIVERSE = "ORB Universe";
    private static final int UNIVERSE_SIZE = 405; // picks = universe size for liberal runs

    /**
     * Defines a single-side scenario for liberal (no-filter) feature collection.
     */
    private record Scenario(
            String name,
            boolean isLong,
            RotationalBacktestConfig.GapDirectionMode gapMode
    ) {}

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("Long-Aligned", true, RotationalBacktestConfig.GapDirectionMode.ALIGNED),
            new Scenario("Long-Opposite", true, RotationalBacktestConfig.GapDirectionMode.OPPOSITE),
            new Scenario("Short-Aligned", false, RotationalBacktestConfig.GapDirectionMode.ALIGNED),
            new Scenario("Short-Opposite", false, RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
    );

    /**
     * Build a maximally liberal config — no filters, picks = universe size.
     * Only stop/target/timing parameters are kept at reasonable defaults since
     * those define the trade mechanics, not the entry filtering.
     */
    private static RotationalBacktestConfig buildLiberalConfig(Scenario s) {
        var builder = RotationalBacktestConfig.builder()
                .universeGroupName(UNIVERSE)
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                // Timing
                .entryCutoffTime(LocalTime.of(15, 15))  // very late cutoff to capture all
                .exitTime(LocalTime.of(15, 20))
                // Stop/Target — kept at reasonable defaults for trade mechanics
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.0)
                .trailingStopEnabled(false)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.0)
                // No slippage for feature analysis (we want raw P&L)
                .slippage(0.0)
                .initialCapital(1_000_000)
                .maxReEntries(0)
                // No filters at all
                .minGapAtr(null)
                .maxGapAtr(null)
                .minOrAtr(null).maxOrAtr(null)
                .minOrbRvol(null).maxOrbRvol(null)
                .minRsRank(null).maxRsRank(null)
                .minOrbIbs(null).maxOrbIbs(null)
                // Trade ALL breakouts (no ranking filter, no pick limit)
                .rankerType(RotationalBacktestConfig.RankerType.NONE)
                .gapDirectionMode(s.gapMode())
                .atrScaling(false);

        // Side and picks
        builder.side(s.isLong() ? RotationalBacktestConfig.Side.LONG : RotationalBacktestConfig.Side.SHORT);
        builder.picks(UNIVERSE_SIZE);

        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Single-Side Feature Analysis Driver");
        log.info("══════════════════════════════════════════════════════════════");
        log.info("");

        // ── Load data ────────────────────────────────────────────────
        log.info("Loading universe from group '{}'...", UNIVERSE);
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(UNIVERSE);
        log.info("Universe contains {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        log.info("Loading 5min bars...");
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        log.info("Loaded intraday bars for {} scrips", intradayBars.size());

        log.info("Loading daily bars...");
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded daily bars for {} scrips", dailyBars.size());
        log.info("");

        // ── Features to compute ──────────────────────────────────────
        int orBarCount = 1; // 5min / 5min = 1

        // ── Output directory ─────────────────────────────────────────
        Path analysisDir = Path.of(System.getProperty("user.home"),
                ".whiteowl", "analysis", "single_side");
        Files.createDirectories(analysisDir);

        // ── Run each scenario ────────────────────────────────────────
        for (Scenario scenario : SCENARIOS) {
            log.info("██████████████████████████████████████████████████████████████████████████████████████");
            log.info("  SCENARIO: {}", scenario.name());
            log.info("██████████████████████████████████████████████████████████████████████████████████████");
            log.info("");

            RotationalBacktestConfig config = buildLiberalConfig(scenario);
            config.validate();
            log.info("{}", config);
            log.info("");

            // Run backtest
            long start = System.currentTimeMillis();
            RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
            long elapsed = System.currentTimeMillis() - start;

            List<RotationalTrade> trades = result.getTradeLog();
            RotationalMetrics metrics = result.getMetrics();

            log.info("Backtest completed in {} ms — {} trades", elapsed, trades.size());
            log.info(String.format("  Win rate: %.1f%%  Avg PnL: %.0f  Total PnL: %.0f",
                    metrics.getWinRate() * 100,
                    trades.stream().mapToDouble(RotationalTrade::netPnl).average().orElse(0),
                    trades.stream().mapToDouble(RotationalTrade::netPnl).sum()));
            log.info("");

            if (trades.isEmpty()) {
                log.warn("No trades for scenario {}. Skipping.", scenario.name());
                continue;
            }

            // Compute features
            List<TradeFeature> features = List.of(
                    new GapAtrFeature(),
                    new OrbRvolFeature(),
                    new RsRankOrOnlyFeature(),
                    new OrbRangeAtrFeature(),
                    new OrIbsFeature()
            );

            TradeFeatureCollector collector = new TradeFeatureCollector(
                    features, dailyBars, intradayBars, orBarCount);

            // Export CSV
            String safeName = scenario.name().replaceAll("[^a-zA-Z0-9_-]", "_");
            Path csvPath = analysisDir.resolve("features_" + safeName + ".csv");
            collector.collectAndWriteCsv(trades, csvPath);
            log.info("Feature CSV exported: {}", csvPath);
            log.info("");

            // Run bucket analysis on each feature
            List<Map<String, Double>> featureValues = collector.collect(trades);

            List<FeatureAnalyzer> analyzers = List.of(
                    new BucketAnalyzer("GAP_ATR", 0.20,
                            -3.0, 3.0, 0.10,   // min sweep
                            -3.0, 5.0, 0.10,   // max sweep
                            20),
                    new BucketAnalyzer("OR_RVOL", 0.25,
                            0.0, 5.0, 0.25,    // min sweep
                            0.5, 10.0, 0.25,   // max sweep
                            20),
                    new BucketAnalyzer("RS_RANK_OR_ONLY", 5.0,
                            0.0, 90.0, 5.0,    // min sweep (percentile 0-100)
                            10.0, 100.0, 5.0,  // max sweep
                            20),
                    new BucketAnalyzer("OR_RANGE_ATR", 0.10,
                            0.0, 1.5, 0.05,    // min sweep
                            0.3, 3.0, 0.10,    // max sweep
                            20),
                    new BucketAnalyzer("OR_IBS", 0.10,
                            0.0, 0.9, 0.05,    // min sweep
                            0.1, 1.0, 0.05,    // max sweep
                            20)
            );

            for (FeatureAnalyzer analyzer : analyzers) {
                analyzer.analyze(trades, featureValues);
            }

            log.info("");
        }

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Analysis complete. CSV files in: {}", analysisDir);
        log.info("══════════════════════════════════════════════════════════════");
    }
}
