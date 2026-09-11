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
 * Analyzes the distance between OR close and OR low (lower wick) for
 * short-side trades produced by the default strategy configuration.
 *
 * <p>This analysis addresses fill probability for short entries: when
 * placing limit sell orders at OR low after seeing the OR close, how far
 * does price need to drop to fill the order?</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.rotational.feature.OrCloseToLowAnalysisDriver
 * </pre>
 */
public final class OrCloseToLowAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(OrCloseToLowAnalysisDriver.class);

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

        // ── Feature ───────────────────────────────────────────────────
        List<TradeFeature> features = List.of(new OrCloseToLowDistanceFeature());

        // ── Analyzer ──────────────────────────────────────────────────
        List<FeatureAnalyzer> analyzers = List.of(
                new BucketAnalyzer("OR_CLOSE_LOW_PCT", 0.10,
                        0.0, 3.0, 0.05,    // min sweep
                        0.1, 5.0, 0.05,    // max sweep
                        20)
        );

        log.info("==============================================================");
        log.info("  OR Close-to-Low Distance Analysis (Short-Side Fill)");
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
                "analysis", "or_close_low_distance.csv");
        Files.createDirectories(csvPath.getParent());
        collector.collectAndWriteCsv(result.getTradeLog(), csvPath);
        log.info("");

        // ── Analyze ───────────────────────────────────────────────────
        List<Map<String, Double>> featureValues = collector.collect(result.getTradeLog());

        // Filter short trades
        List<RotationalTrade> shortTrades = result.getTradeLog().stream()
                .filter(t -> t.side() == RotationalTrade.Side.SHORT).toList();
        List<RotationalTrade> longTrades = result.getTradeLog().stream()
                .filter(t -> t.side() == RotationalTrade.Side.LONG).toList();

        // Overall analysis
        log.info("==============================================================");
        log.info("  ALL TRADES");
        log.info("==============================================================");
        for (FeatureAnalyzer analyzer : analyzers) {
            analyzer.analyze(result.getTradeLog(), featureValues);
        }

        // Short-side analysis (most relevant for limit sell at OR low)
        log.info("==============================================================");
        log.info("  SHORT TRADES ONLY (limit sell at OR low)");
        log.info("==============================================================");
        List<Map<String, Double>> shortFeatures = new ArrayList<>();
        for (int i = 0; i < result.getTradeLog().size(); i++) {
            if (result.getTradeLog().get(i).side() == RotationalTrade.Side.SHORT) {
                shortFeatures.add(featureValues.get(i));
            }
        }
        for (FeatureAnalyzer analyzer : analyzers) {
            analyzer.analyze(shortTrades, shortFeatures);
        }

        // Long-side analysis (for completeness)
        log.info("==============================================================");
        log.info("  LONG TRADES ONLY (for reference)");
        log.info("==============================================================");
        List<Map<String, Double>> longFeatures = new ArrayList<>();
        for (int i = 0; i < result.getTradeLog().size(); i++) {
            if (result.getTradeLog().get(i).side() == RotationalTrade.Side.LONG) {
                longFeatures.add(featureValues.get(i));
            }
        }
        for (FeatureAnalyzer analyzer : analyzers) {
            analyzer.analyze(longTrades, longFeatures);
        }

        // ── Custom fill probability analysis ──────────────────────────
        printFillProbabilityAnalysis(result.getTradeLog(), featureValues);

        log.info("CSV output: {}", csvPath);
    }

    /**
     * Print a custom analysis focused on short-side fill probability.
     */
    private static void printFillProbabilityAnalysis(
            List<RotationalTrade> trades, List<Map<String, Double>> featureValues) {

        log.info("");
        log.info("==============================================================");
        log.info("  Short-Side Fill Probability Analysis");
        log.info("  (How far is OR close from OR low, as % of OR close)");
        log.info("==============================================================");
        log.info("");

        // Collect valid values
        List<Double> allValues = new ArrayList<>();
        List<Double> longValues = new ArrayList<>();
        List<Double> shortValues = new ArrayList<>();

        for (int i = 0; i < trades.size(); i++) {
            Double val = featureValues.get(i).get("OR_CLOSE_LOW_PCT");
            if (val == null || Double.isNaN(val)) continue;
            allValues.add(val);
            if (trades.get(i).side() == RotationalTrade.Side.LONG) {
                longValues.add(val);
            } else {
                shortValues.add(val);
            }
        }

        if (allValues.isEmpty()) {
            log.info("No valid data.");
            return;
        }

        double[] all = allValues.stream().mapToDouble(Double::doubleValue).toArray();
        double[] longs = longValues.stream().mapToDouble(Double::doubleValue).toArray();
        double[] shorts = shortValues.stream().mapToDouble(Double::doubleValue).toArray();

        Arrays.sort(all);
        Arrays.sort(longs);
        Arrays.sort(shorts);

        log.info("{}", String.format("%-20s %10s %10s %10s", "", "All", "Long", "Short"));
        log.info("{}", String.format("%-20s %10d %10d %10d", "Count", all.length, longs.length, shorts.length));
        log.info("{}", String.format("%-20s %10s %10s %10s", "Mean",
                pct(mean(all)), pct(mean(longs)), pct(mean(shorts))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "Median",
                pct(percentile(all, 50)), pct(percentile(longs, 50)), pct(percentile(shorts, 50))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "P10",
                pct(percentile(all, 10)), pct(percentile(longs, 10)), pct(percentile(shorts, 10))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "P25",
                pct(percentile(all, 25)), pct(percentile(longs, 25)), pct(percentile(shorts, 25))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "P75",
                pct(percentile(all, 75)), pct(percentile(longs, 75)), pct(percentile(shorts, 75))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "P90",
                pct(percentile(all, 90)), pct(percentile(longs, 90)), pct(percentile(shorts, 90))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "P95",
                pct(percentile(all, 95)), pct(percentile(longs, 95)), pct(percentile(shorts, 95))));
        log.info("{}", String.format("%-20s %10s %10s %10s", "Max",
                pct(percentile(all, 100)), pct(percentile(longs, 100)), pct(percentile(shorts, 100))));
        log.info("");

        // Cumulative distribution: what % of trades have distance <= X%
        log.info("── Cumulative Distribution (% of trades with distance <= threshold) ──");
        log.info("{}", String.format("%-20s %10s %10s %10s", "Distance <=", "All", "Long", "Short"));
        double[] thresholds = {0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.40, 0.50, 0.75, 1.00, 1.50, 2.00, 3.00};
        for (double t : thresholds) {
            log.info("{}", String.format("%-20s %10s %10s %10s",
                    String.format("%.2f%%", t),
                    pct(cumulativePct(all, t)),
                    pct(cumulativePct(longs, t)),
                    pct(cumulativePct(shorts, t))));
        }
        log.info("");

        log.info("── Interpretation ──────────────────────────────────────────");
        log.info("For SHORT entries (primary focus):");
        log.info("  - We place limit sell at OR low after seeing OR close");
        log.info("  - OR_CLOSE_LOW_PCT = how far above OR low the OR closed");
        log.info("  - Small values = OR closed near low = limit likely fills quickly");
        log.info("  - Large values = OR closed well above low = needs bigger drop to fill");
        log.info("");
    }

    private static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 0) return 0;
        double idx = (p / 100.0) * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = Math.min((int) Math.ceil(idx), sorted.length - 1);
        if (lo == hi) return sorted[lo];
        return sorted[lo] + (idx - lo) * (sorted[hi] - sorted[lo]);
    }

    private static double cumulativePct(double[] sorted, double threshold) {
        if (sorted.length == 0) return 0;
        int count = 0;
        for (double v : sorted) {
            if (v <= threshold) count++;
            else break;
        }
        return (double) count / sorted.length * 100.0;
    }

    private static String pct(double value) {
        return String.format("%.2f%%", value);
    }
}
