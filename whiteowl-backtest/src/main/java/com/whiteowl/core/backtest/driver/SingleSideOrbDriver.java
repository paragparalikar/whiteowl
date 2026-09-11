package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
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
 * Single-side ORB backtest driver — runs the strategy for one side at a time.
 *
 * <p>This driver executes 4 distinct scenarios for analysis:</p>
 * <ol>
 *   <li><b>Long + Aligned</b> — go long on gap-up breakouts (continuation)</li>
 *   <li><b>Long + Opposite</b> — go long on gap-down breakouts (contrarian longs)</li>
 *   <li><b>Short + Aligned</b> — go short on gap-down breakouts (continuation shorts)</li>
 *   <li><b>Short + Opposite</b> — go short on gap-up breakouts (fading the gap)</li>
 * </ol>
 *
 * <p>Each scenario uses:</p>
 * <ul>
 *   <li>A gap filter with optimizable min/max gap/ATR values</li>
 *   <li>An RS Rank filter with optimizable min/max values</li>
 *   <li>An RVOL filter with optimizable min/max values</li>
 *   <li>Alphabetical sorting for deterministic pick selection</li>
 *   <li>Configurable number of picks per side</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.SingleSideOrbDriver
 * </pre>
 */
public final class SingleSideOrbDriver {

    private static final Logger log = LoggerFactory.getLogger(SingleSideOrbDriver.class);

    // ══════════════════════════════════════════════════════════════════
    //  SHARED CONFIGURATION — common parameters across all scenarios
    // ══════════════════════════════════════════════════════════════════

    private static final String UNIVERSE = "ORB Universe";
    private static final int OPENING_RANGE_MINUTES = 5;
    private static final int BAR_MINUTES = 5;
    private static final RotationalBacktestConfig.EntryMethod ENTRY_METHOD =
            RotationalBacktestConfig.EntryMethod.BREAKOUT;
    private static final LocalTime ENTRY_CUTOFF = LocalTime.of(10, 30);
    private static final LocalTime EXIT_TIME = LocalTime.of(15, 20);
    private static final double STOP_MULTIPLIER = 0.70;
    private static final RotationalBacktestConfig.StopBasis STOP_BASIS =
            RotationalBacktestConfig.StopBasis.OR_RANGE;
    private static final double TRAILING_STOP_MULTIPLIER = 1.50;
    private static final RotationalBacktestConfig.StopBasis TRAILING_STOP_BASIS =
            RotationalBacktestConfig.StopBasis.ATR;
    private static final double TARGET_MULTIPLIER = 5.25;
    private static final RotationalBacktestConfig.TargetBasis TARGET_BASIS =
            RotationalBacktestConfig.TargetBasis.STOP_DISTANCE;
    private static final double SLIPPAGE = 0.002;
    private static final double INITIAL_CAPITAL = 1_000_000;
    private static final int MAX_RE_ENTRIES = 1;
    private static final Double MAX_ORB_IBS = 0.80;

    // ══════════════════════════════════════════════════════════════════
    //  PER-SCENARIO CONFIGURATION
    // ══════════════════════════════════════════════════════════════════

    /**
     * Defines a single-side backtest scenario.
     */
    private record Scenario(
            String name,
            boolean isLong,
            RotationalBacktestConfig.GapDirectionMode gapMode,
            int picks,
            Double minGapAtr,
            Double maxGapAtr,
            Double minRsRank,
            Double maxRsRank,
            Double minRvol,
            Double maxRvol
    ) {}

    /**
     * Build the 4 scenarios to test.
     * Adjust the filter ranges here for different analysis runs.
     */
    private static List<Scenario> buildScenarios() {
        return List.of(
                // 1. Long + Aligned (gap-up continuation)
                new Scenario("Long-Aligned (gap-up continuation)", true,
                        RotationalBacktestConfig.GapDirectionMode.ALIGNED,
                        4,      // picks
                        0.40,   // minGapAtr
                        null,   // maxGapAtr (no max)
                        null,   // minRsRank
                        null,   // maxRsRank
                        null,   // minRvol
                        null),  // maxRvol

                // 2. Long + Opposite (long on gap-down — contrarian)
                new Scenario("Long-Opposite (gap-down contrarian)", true,
                        RotationalBacktestConfig.GapDirectionMode.OPPOSITE,
                        4,
                        null,   // minGapAtr (gap-down stocks, so signed gap is negative)
                        null,   // maxGapAtr
                        null,
                        null,
                        null,
                        null),

                // 3. Short + Aligned (gap-down continuation)
                new Scenario("Short-Aligned (gap-down continuation)", false,
                        RotationalBacktestConfig.GapDirectionMode.ALIGNED,
                        4,
                        null,   // minGapAtr (gap-down stocks)
                        null,
                        null,
                        null,
                        null,
                        null),

                // 4. Short + Opposite (short on gap-up — fading the gap)
                new Scenario("Short-Opposite (gap-up failure)", false,
                        RotationalBacktestConfig.GapDirectionMode.OPPOSITE,
                        4,
                        0.60,   // minGapAtr (require strong gap-up to fade)
                        null,
                        null,
                        null,
                        null,
                        null)
        );
    }

    /**
     * Build a RotationalBacktestConfig for a given scenario.
     */
    private static RotationalBacktestConfig buildConfig(Scenario s) {
        var builder = RotationalBacktestConfig.builder()
                .universeGroupName(UNIVERSE)
                .openingRangeMinutes(OPENING_RANGE_MINUTES)
                .barMinutes(BAR_MINUTES)
                .entryMethod(ENTRY_METHOD)
                .entryCutoffTime(ENTRY_CUTOFF)
                .exitTime(EXIT_TIME)
                .stopBasis(STOP_BASIS)
                .stopMultiplier(STOP_MULTIPLIER)
                .trailingStopEnabled(true)
                .trailingStopBasis(TRAILING_STOP_BASIS)
                .trailingStopMultiplier(TRAILING_STOP_MULTIPLIER)
                .targetEnabled(true)
                .targetBasis(TARGET_BASIS)
                .targetMultiplier(TARGET_MULTIPLIER)
                .slippage(SLIPPAGE)
                .initialCapital(INITIAL_CAPITAL)
                .maxReEntries(MAX_RE_ENTRIES)
                .maxOrbIbs(MAX_ORB_IBS)
                .atrScaling(false)
                // Alphabetical sorting for deterministic pick selection
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                // Gap direction mode
                .gapDirectionMode(s.gapMode());

        // Side and picks
        builder.side(s.isLong() ? RotationalBacktestConfig.Side.LONG : RotationalBacktestConfig.Side.SHORT);
        builder.picks(s.picks());

        // Gap filter
        builder.minGapAtr(s.minGapAtr()).maxGapAtr(s.maxGapAtr());

        // RS Rank filter
        builder.minRsRank(s.minRsRank()).maxRsRank(s.maxRsRank());

        // RVOL filter
        builder.minOrbRvol(s.minRvol()).maxOrbRvol(s.maxRvol());

        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Single-Side ORB Backtest Driver");
        log.info("══════════════════════════════════════════════════════════════");
        log.info("");

        // ── Load data ────────────────────────────────────────────────
        log.info("Loading universe from group '{}'...", UNIVERSE);
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(UNIVERSE);
        log.info("Universe contains {} scrips", scripIds.size());

        Timeframe barTimeframe = Timeframe.FIVE_MINUTE;
        log.info("Loading {} bars...", barTimeframe.getLabel());
        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, barTimeframe);
        log.info("Loaded intraday bars for {} scrips", intradayBars.size());

        log.info("Loading daily bars...");
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded daily bars for {} scrips", dailyBars.size());

        // Log date range
        long minTs = Long.MAX_VALUE, maxTs = Long.MIN_VALUE;
        for (Bars bars : intradayBars.values()) {
            if (bars.size() > 0) {
                minTs = Math.min(minTs, bars.getTimestamp(0));
                maxTs = Math.max(maxTs, bars.getTimestamp(bars.size() - 1));
            }
        }
        log.info("Data range: {} to {}",
                java.time.Instant.ofEpochMilli(minTs).atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate(),
                java.time.Instant.ofEpochMilli(maxTs).atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate());
        log.info("");

        // ── Run each scenario ────────────────────────────────────────
        List<Scenario> scenarios = buildScenarios();
        List<ScenarioResult> results = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            log.info("══════════════════════════════════════════════════════════════");
            log.info("  Scenario: {}", scenario.name());
            log.info("══════════════════════════════════════════════════════════════");

            RotationalBacktestConfig config = buildConfig(scenario);
            config.validate();
            log.info("{}", config);
            log.info("");

            long start = System.currentTimeMillis();
            RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
            long elapsed = System.currentTimeMillis() - start;

            RotationalMetrics metrics = result.getMetrics();
            results.add(new ScenarioResult(scenario, config, result, metrics));

            log.info("Completed in {} ms", elapsed);
            log.info("");

            // Print per-scenario report
            RotationalBacktestReport report = RotationalBacktestReport.fromResult(result, config);
            report.printToConsole();
            log.info("");
        }

        // ── Comparison summary ───────────────────────────────────────
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  SCENARIO COMPARISON SUMMARY");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info(String.format("  %-38s %8s %8s %8s %8s %7s %10s %6s",
                "Scenario", "Sharpe", "Sortino", "CAGR%", "MaxDD%", "Trades", "Net PnL", "WR%"));
        log.info("  " + "-".repeat(105));

        for (ScenarioResult sr : results) {
            RotationalMetrics m = sr.metrics();
            log.info(String.format("  %-38s %8.2f %8.2f %7.1f%% %7.1f%% %7d %10s %5.1f%%",
                    sr.scenario().name(),
                    m.getSharpe(), m.getSortino(),
                    m.getCagr() * 100, m.getMaxDrawdown() * 100,
                    m.getTotalTrades(),
                    fmtPnl(m.getLongPnl() + m.getShortPnl()),
                    m.getWinRate() * 100));
        }
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");

        // ── Export equity curves ──────────────────────────────────────
        Path outputDir = Path.of(System.getProperty("user.home"), ".whiteowl", "output");
        Files.createDirectories(outputDir);
        for (ScenarioResult sr : results) {
            String safeName = sr.scenario().name().replaceAll("[^a-zA-Z0-9_-]", "_");
            Path eqCsvPath = outputDir.resolve("equity_curve_" + safeName + ".csv");
            sr.result().equityCurveToCsv(eqCsvPath);
            log.info("Equity curve exported: {}", eqCsvPath);
        }
    }

    private record ScenarioResult(Scenario scenario, RotationalBacktestConfig config,
                                  RotationalBacktestResult result, RotationalMetrics metrics) {}

    private static String fmtPnl(double value) {
        if (Math.abs(value) >= 100_000) return String.format("%.2fL", value / 100_000);
        return String.format("%.0f", value);
    }
}
