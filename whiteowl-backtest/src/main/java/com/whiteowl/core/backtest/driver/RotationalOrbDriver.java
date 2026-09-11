package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
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
import java.util.List;

/**
 * Driver for the rotational ORB backtest.
 *
 * <p>This is the main entry point that wires together:</p>
 * <ol>
 *   <li><b>Configuration</b> — {@link RotationalBacktestConfig} with all ORB parameters.</li>
 *   <li><b>Data loading</b> — reads the group file (universe), loads intraday bars for each scrip.</li>
 *   <li><b>Feature observation</b> — optional hook to compute and log features for analysis.</li>
 *   <li><b>Engine</b> — {@link RotationalBacktestEngine} runs the day-by-day simulation.</li>
 *   <li><b>Report</b> — {@link RotationalBacktestReport} prints the full report to the console.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.rotational.RotationalOrbDriver
 * </pre>
 */
public final class RotationalOrbDriver {

    private static final Logger log = LoggerFactory.getLogger(RotationalOrbDriver.class);

    // ══════════════════════════════════════════════════════════════════
    //  CONFIGURATION — Edit these to change backtest parameters
    // ══════════════════════════════════════════════════════════════════

    private static RotationalBacktestConfig buildConfig() {
        return RotationalBacktestConfig.builder()
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
                .slippage(0.002)
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();
    }

    /** Enable feature observation and logging (set true to compute features). */
    private static final boolean ENABLE_FEATURES = false;

    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        RotationalBacktestConfig config = buildConfig();
        config.validate();

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Rotational ORB Backtest Driver");
        log.info("══════════════════════════════════════════════════════════════");
        log.info("");
        log.info("{}", config);
        log.info("");

        // ── Step 1: Load universe from group file ────────────────────
        log.info("Loading universe from group '{}'...", config.getUniverseGroupName());
        List<String> scripIds = loadGroupScrips(config.getUniverseGroupName());
        log.info("Universe contains {} scrips", scripIds.size());

        // ── Step 2: Load intraday bars for each scrip ────────────────
        Timeframe barTimeframe = Timeframe.FIVE_MINUTE;
        log.info("Loading {} bars for {} scrips...", barTimeframe.getLabel(), scripIds.size());
        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = loadIntradayBars(barsRepo, scripIds, barTimeframe);
        log.info("Loaded intraday bars for {} scrips (skipped {} without data)",
                intradayBars.size(), scripIds.size() - intradayBars.size());

        if (intradayBars.isEmpty()) {
            log.error("No intraday data found. Exiting.");
            return;
        }

        // ── Step 2b: Load daily bars (for entry filters and ranker) ─────
        log.info("Loading daily bars...");
        Map<String, Bars> dailyBars = loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
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

        // ── Step 3: Set up feature observer (optional) ───────────────
        RotationalBacktestEngine.FeatureObserver featureObserver = null;
        if (ENABLE_FEATURES) {
            featureObserver = (date, orbResults, daySlices) -> {
                if (!orbResults.isEmpty()) {
                    long longBreakouts = orbResults.values().stream()
                            .filter(r -> r.side() == RotationalTrade.Side.LONG).count();
                    long shortBreakouts = orbResults.values().stream()
                            .filter(r -> r.side() == RotationalTrade.Side.SHORT).count();
                    log.debug("Features [{}]: {} symbols, {} breakouts ({} long, {} short)",
                            date, daySlices.size(), orbResults.size(), longBreakouts, shortBreakouts);
                }
            };
        }

        // ── Step 4: Run the backtest ─────────────────────────────────
        log.info("Running backtest...");
        long start = System.currentTimeMillis();

        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, featureObserver);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Backtest completed in {} ms", elapsed);
        log.info("");

        // ── Step 5: Build and print report ───────────────────────────
        RotationalBacktestReport report = RotationalBacktestReport.fromResult(result, config);
        report.printToConsole();

        // ── Step 6: Export equity curve CSV ──────────────────────────
        Path outputDir = Path.of(System.getProperty("user.home"), ".whiteowl", "output");
        Files.createDirectories(outputDir);
        Path eqCsvPath = outputDir.resolve("equity_curve.csv");
        result.equityCurveToCsv(eqCsvPath);
        log.info("Equity curve exported to: {}", eqCsvPath);

        // ── Step 7: Print text-based equity curve ────────────────────
        printEquityCurveChart(result.getPortfolio(), config.getInitialCapital());
    }

    /**
     * Print a text-based equity curve chart to the console.
     */
    private static void printEquityCurveChart(PortfolioTracker portfolio, double initialCapital) {
        List<Long> dates = portfolio.getDates();
        List<Double> equity = portfolio.getEquityCurve();
        List<Double> drawdown = portfolio.getDrawdownCurve();

        if (dates.isEmpty()) return;

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");

        // Find range
        double minEq = equity.stream().mapToDouble(Double::doubleValue).min().orElse(initialCapital);
        double maxEq = equity.stream().mapToDouble(Double::doubleValue).max().orElse(initialCapital);

        int chartWidth = 80;
        int chartHeight = 25;

        log.info("");
        log.info("══════════════════════════════════════════════════════════════════════════════════════════════");
        log.info("  EQUITY CURVE");
        log.info("══════════════════════════════════════════════════════════════════════════════════════════════");
        log.info(String.format("  Start: %s  End: %s  Days: %d",
                java.time.Instant.ofEpochMilli(dates.getFirst()).atZone(ist).toLocalDate(),
                java.time.Instant.ofEpochMilli(dates.getLast()).atZone(ist).toLocalDate(),
                dates.size()));
        log.info(String.format("  Initial: %,.0f  Final: %,.0f  Peak: %,.0f  Trough: %,.0f",
                initialCapital, equity.getLast(), maxEq, minEq));
        log.info("");

        // Build chart grid
        char[][] grid = new char[chartHeight][chartWidth];
        for (char[] row : grid) java.util.Arrays.fill(row, ' ');

        // Plot equity on the grid — sample points
        for (int col = 0; col < chartWidth; col++) {
            int idx = (int) ((long) col * (equity.size() - 1) / (chartWidth - 1));
            double val = equity.get(idx);
            int row = (int) ((maxEq - val) / (maxEq - minEq + 1) * (chartHeight - 1));
            row = Math.max(0, Math.min(chartHeight - 1, row));
            grid[row][col] = '\u2588'; // full block

            // Fill below with lighter shade for area effect
            for (int r = row + 1; r < chartHeight; r++) {
                if (grid[r][col] == ' ') grid[r][col] = '\u2591'; // light shade
            }
        }

        // Print with Y-axis labels
        for (int row = 0; row < chartHeight; row++) {
            double yVal = maxEq - (double) row / (chartHeight - 1) * (maxEq - minEq);
            String label;
            if (row == 0 || row == chartHeight - 1 || row == chartHeight / 2) {
                label = String.format("%,10.0f", yVal);
            } else {
                label = "          ";
            }
            log.info(String.format("%s |%s", label, new String(grid[row])));
        }

        // X-axis
        String startDate = java.time.Instant.ofEpochMilli(dates.getFirst()).atZone(ist).format(fmt);
        String endDate = java.time.Instant.ofEpochMilli(dates.getLast()).atZone(ist).format(fmt);
        String midDate = java.time.Instant.ofEpochMilli(dates.get(dates.size() / 2)).atZone(ist).format(fmt);
        log.info(String.format("%s +%s", "          ", "\u2500".repeat(chartWidth)));
        log.info(String.format("%s  %-26s%-27s%s", "          ", startDate, midDate, endDate));

        // ── Drawdown chart ───────────────────────────────────────────
        log.info("");
        log.info("\u2500\u2500 DRAWDOWN \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        double minDD = drawdown.stream().mapToDouble(Double::doubleValue).min().orElse(0);

        int ddHeight = 10;
        char[][] ddGrid = new char[ddHeight][chartWidth];
        for (char[] row : ddGrid) java.util.Arrays.fill(row, ' ');

        for (int col = 0; col < chartWidth; col++) {
            int idx = (int) ((long) col * (drawdown.size() - 1) / (chartWidth - 1));
            double dd = drawdown.get(idx);
            int row = (int) (dd / (minDD - 1e-9) * (ddHeight - 1));
            row = Math.max(0, Math.min(ddHeight - 1, row));
            for (int r = 0; r <= row; r++) {
                ddGrid[r][col] = '\u2593'; // dark shade
            }
        }

        for (int row = 0; row < ddHeight; row++) {
            double yVal = (double) row / (ddHeight - 1) * minDD;
            String label;
            if (row == 0) {
                label = "     0.0% ";
            } else if (row == ddHeight - 1) {
                label = String.format("%9.1f%%", minDD * 100);
            } else {
                label = "          ";
            }
            log.info(String.format("%s |%s", label, new String(ddGrid[row])));
        }
        log.info(String.format("%s +%s", "          ", "\u2500".repeat(chartWidth)));
        log.info(String.format("%s  %-26s%-27s%s", "          ", startDate, midDate, endDate));
    }

    // ── Data loading helpers ─────────────────────────────────────────

    /**
     * Load scrip IDs from a group CSV file.
     */
    public static List<String> loadGroupScrips(String groupName) throws IOException {
        String fileName = groupName.replaceAll("[^a-zA-Z0-9._\\- ]", "_") + ".csv";
        Path groupFile = Path.of(System.getProperty("whiteowl.home",
                        System.getProperty("user.home") + java.io.File.separator + ".whiteowl"),
                "data", "groups", fileName);

        if (!Files.exists(groupFile)) {
            throw new IOException("Group file not found: " + groupFile
                    + ". Create the group in the workbench first.");
        }

        List<String> scripIds = Files.readAllLines(groupFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (scripIds.isEmpty()) {
            throw new IOException("Group '" + groupName + "' is empty.");
        }

        return scripIds;
    }

    /**
     * Load bars for each scrip ID, skipping scrips without data.
     */
    public static Map<String, Bars> loadIntradayBars(BarsRepository barsRepo,
                                               List<String> scripIds,
                                               Timeframe timeframe) {
        Map<String, Bars> result = new LinkedHashMap<>();
        for (String scripId : scripIds) {
            try {
                if (!barsRepo.exists(scripId, timeframe)) continue;
                Bars bars = barsRepo.load(scripId, timeframe);
                if (bars != null && bars.size() > 0) {
                    result.put(scripId, bars);
                }
            } catch (IOException e) {
                // skip silently
            }
        }
        return result;
    }
}
