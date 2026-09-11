package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.feature.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

/**
 * Runs the ORB backtest and performs multi-feature analysis to determine
 * optimal filter thresholds.
 *
 * <p>Each {@link TradeFeature} is computed for every trade and then analyzed
 * by a corresponding {@link FeatureAnalyzer} (bucket analysis, sweeps,
 * winner/loser comparison, etc.).</p>
 *
 * <p>To add a new feature for analysis:</p>
 * <ol>
 *   <li>Create a class implementing {@link TradeFeature}.</li>
 *   <li>Add it to the {@code features} list below.</li>
 *   <li>Optionally create a custom {@link FeatureAnalyzer} or reuse
 *       {@link BucketAnalyzer} with appropriate bucket width and sweep ranges.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.rotational.feature.OrbFeatureAnalysisDriver
 * </pre>
 */
public final class OrbFeatureAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(OrbFeatureAnalysisDriver.class);

    public static void main(String[] args) throws Exception {
        // ── Configuration ────────────────────────────────────────────────
        RotationalBacktestConfig config = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.0)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(1.5)
                .entryCutoffTime(LocalTime.of(14, 0))
                .rankerType(RotationalBacktestConfig.RankerType.NONE)
                .picks(5)
                .slippage(0.001)
                .initialCapital(1_000_000)
                .build();
        config.validate();

        Timeframe barTimeframe = Timeframe.FIVE_MINUTE;
        int barMinutes = 5;
        int orBarCount = Math.max(1, config.getOpeningRangeMinutes() / barMinutes);

        // ── Features to analyze ──────────────────────────────────────────
        List<TradeFeature> features = List.of(
                new OrbRangeAtrFeature(),
                new GapAtrFeature(),
                new OrbRvolFeature(),
                new RsRankCloseToOrFeature(),
                new RsRankOrOnlyFeature()
        );

        // ── Analyzers (one per feature) ──────────────────────────────────
        List<FeatureAnalyzer> analyzers = List.of(
                new BucketAnalyzer("OR_RANGE_ATR", 0.10,
                        0.0, 1.5, 0.1,    // min sweep
                        0.5, 3.0, 0.1,    // max sweep
                        20),
                new BucketAnalyzer("GAP_ATR", 0.10,
                        -3.0, 3.0, 0.1,   // min sweep (gaps can be negative)
                        -3.0, 3.0, 0.1,   // max sweep
                        20),
                new BucketAnalyzer("OR_RVOL", 0.25,
                        0.0, 5.0, 0.25,   // min sweep
                        0.5, 10.0, 0.25,  // max sweep
                        20),
                new BucketAnalyzer("RS_RANK_CLOSE_TO_OR", 5.0,
                        0.0, 90.0, 5.0,   // min sweep (percentile 0-100)
                        10.0, 100.0, 5.0, // max sweep
                        20),
                new BucketAnalyzer("RS_RANK_OR_ONLY", 5.0,
                        0.0, 90.0, 5.0,   // min sweep
                        10.0, 100.0, 5.0, // max sweep
                        20)
        );

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  ORB Multi-Feature Analysis");
        log.info("══════════════════════════════════════════════════════════════");
        log.info("{}", config);
        log.info("Features: {}", features.stream().map(TradeFeature::name).toList());
        log.info("");

        // ── Step 1: Load universe ────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(config.getUniverseGroupName());
        log.info("Universe: {} scrips", scripIds.size());

        // ── Step 2: Load intraday + daily bars ───────────────────────────
        BarsRepository barsRepo = new FileBarsRepository();

        log.info("Loading intraday ({}) bars...", barTimeframe.getLabel());
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, barTimeframe);
        log.info("Loaded intraday bars for {} scrips", intradayBars.size());

        log.info("Loading daily bars...");
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded daily bars for {} scrips", dailyBars.size());
        log.info("");

        if (intradayBars.isEmpty()) {
            log.error("No intraday data found. Exiting.");
            return;
        }

        // ── Step 3: Run backtest ─────────────────────────────────────────
        log.info("Running backtest...");
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
        log.info("Backtest produced {} trades", result.getTradeLog().size());
        log.info("");

        // ── Step 4: Compute features and write CSV ───────────────────────
        TradeFeatureCollector collector = new TradeFeatureCollector(
                features, dailyBars, intradayBars, orBarCount);

        Path csvPath = Path.of(System.getProperty("user.home"), ".whiteowl",
                "analysis", "orb_multi_features.csv");
        Files.createDirectories(csvPath.getParent());
        collector.collectAndWriteCsv(result.getTradeLog(), csvPath);
        log.info("");

        // ── Step 5: Analyze each feature ─────────────────────────────────
        List<Map<String, Double>> featureValues = collector.collect(result.getTradeLog());
        for (FeatureAnalyzer analyzer : analyzers) {
            analyzer.analyze(result.getTradeLog(), featureValues);
        }
    }
}
