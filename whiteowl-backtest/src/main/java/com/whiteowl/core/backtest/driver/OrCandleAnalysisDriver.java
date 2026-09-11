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
 * Analyzes OR candle structure features (upper wick, lower wick, body, spread,
 * IBS) for all trades produced by the default strategy configuration.
 *
 * <p>Runs each feature through bucket analysis to determine if any OR candle
 * characteristic can improve strategy performance when used as a filter.</p>
 */
public final class OrCandleAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(OrCandleAnalysisDriver.class);

    public static void main(String[] args) throws Exception {
        // ── Use default strategy configuration ────────────────────────
        RotationalBacktestConfig config = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50)
                .maxOrbIbs(0.90)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(0.8)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.25)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(1.5)
                .entryCutoffTime(LocalTime.of(11, 0))
                .exitTime(LocalTime.of(15, 25))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(0.001)
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();
        config.validate();

        Timeframe barTimeframe = Timeframe.FIVE_MINUTE;
        int barMinutes = 5;
        int orBarCount = Math.max(1, config.getOpeningRangeMinutes() / barMinutes);

        // ── Features ────────────────────────────────────────────────────
        List<TradeFeature> features = List.of(
                new OrUpperWickFeature(),
                new OrLowerWickFeature(),
                new OrBodyFeature(),
                new OrSpreadFeature(),
                new OrIbsFeature()
        );

        // ── Analyzers (one per feature, tuned sweep ranges) ─────────
        List<FeatureAnalyzer> analyzers = List.of(
                // Upper wick: typically 0-3%, bucket 0.10
                new BucketAnalyzer("OR_UPPER_WICK_PCT", 0.10,
                        0.0, 2.0, 0.05,
                        0.2, 5.0, 0.05,
                        20),
                // Lower wick: typically 0-3%, bucket 0.10
                new BucketAnalyzer("OR_LOWER_WICK_PCT", 0.10,
                        0.0, 2.0, 0.05,
                        0.2, 5.0, 0.05,
                        20),
                // Body: typically 0-3%, bucket 0.10
                new BucketAnalyzer("OR_BODY_PCT", 0.10,
                        0.0, 2.0, 0.05,
                        0.2, 5.0, 0.05,
                        20),
                // Spread: typically 0.5-5%, bucket 0.20
                new BucketAnalyzer("OR_SPREAD_PCT", 0.20,
                        0.0, 3.0, 0.10,
                        0.5, 8.0, 0.10,
                        20),
                // IBS: 0 to 0.90 (due to filter), bucket 0.05
                new BucketAnalyzer("OR_IBS", 0.05,
                        0.0, 0.5, 0.05,
                        0.3, 0.95, 0.05,
                        20)
        );

        log.info("==============================================================");
        log.info("  OR Candle Structure Feature Analysis");
        log.info("==============================================================");
        log.info("{}", config);
        log.info("");

        // ── Load universe ─────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(config.getUniverseGroupName());
        log.info("Universe: {} scrips", scripIds.size());

        // ── Load bars ─────────────────────────────────────────────────
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

        // ── Run backtest ──────────────────────────────────────────────
        log.info("Running backtest with default config...");
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
        log.info("Backtest produced {} trades", result.getTradeLog().size());
        log.info("");

        // ── Compute features and write CSV ────────────────────────────
        TradeFeatureCollector collector = new TradeFeatureCollector(
                features, dailyBars, intradayBars, orBarCount);

        Path csvPath = Path.of(System.getProperty("user.home"), ".whiteowl",
                "analysis", "or_candle_features.csv");
        Files.createDirectories(csvPath.getParent());
        collector.collectAndWriteCsv(result.getTradeLog(), csvPath);
        log.info("");

        // ── Compute feature values ──────────────────────────────────
        List<Map<String, Double>> featureValues = collector.collect(result.getTradeLog());

        // Side-specific trade lists
        List<RotationalTrade> allTrades = result.getTradeLog();
        List<RotationalTrade> longTrades = allTrades.stream()
                .filter(t -> t.side() == RotationalTrade.Side.LONG).toList();
        List<RotationalTrade> shortTrades = allTrades.stream()
                .filter(t -> t.side() == RotationalTrade.Side.SHORT).toList();

        List<Map<String, Double>> longFeatures = new ArrayList<>();
        List<Map<String, Double>> shortFeatures = new ArrayList<>();
        for (int i = 0; i < allTrades.size(); i++) {
            if (allTrades.get(i).side() == RotationalTrade.Side.LONG) {
                longFeatures.add(featureValues.get(i));
            } else {
                shortFeatures.add(featureValues.get(i));
            }
        }

        // ── Run analysis per feature ────────────────────────────────
        for (FeatureAnalyzer analyzer : analyzers) {
            log.info("");
            log.info("##############################################################");
            log.info("##  ALL TRADES");
            log.info("##############################################################");
            analyzer.analyze(allTrades, featureValues);

            log.info("");
            log.info("##############################################################");
            log.info("##  LONG TRADES ONLY ({} trades)", longTrades.size());
            log.info("##############################################################");
            analyzer.analyze(longTrades, longFeatures);

            log.info("");
            log.info("##############################################################");
            log.info("##  SHORT TRADES ONLY ({} trades)", shortTrades.size());
            log.info("##############################################################");
            analyzer.analyze(shortTrades, shortFeatures);
        }

        log.info("");
        log.info("==============================================================");
        log.info("  Analysis Complete");
        log.info("==============================================================");
        log.info("CSV output: {}", csvPath);
    }
}
