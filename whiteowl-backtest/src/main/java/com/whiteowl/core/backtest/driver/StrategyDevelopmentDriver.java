package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.discovery.*;
import com.whiteowl.core.bar.model.Bars;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Stage-2 development for the chosen signal ({@code vol_breakout}):
 *
 * <ol>
 *   <li>MFE/MAE excursion analysis on the EOD-exit trades.</li>
 *   <li>Stop x target grid search over bar paths; pick the best stable cell.</li>
 *   <li>Re-run the signal with the chosen stop/target, write the enhanced
 *       trade CSV.</li>
 *   <li>Time-of-day / day-of-week breakdown.</li>
 *   <li>Meta-labeling on the enhanced trades -> report OOS Sortino.</li>
 * </ol>
 */
@Slf4j
public final class StrategyDevelopmentDriver {

    private static final Path INPUT = Path.of(
            "C:\\trading\\OptionsData_Order91_GOLD-GOLDM-and-6-more_Futures_2023-2026",
            "continuous_csv", "CRUDEOIL_continuous_1min.csv");
    private static final String SIGNAL = "vol_breakout";

    // 0 = disabled for that axis
    private static final double[] STOPS = {0, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0};
    private static final double[] TARGETS = {0, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0};

    public static void main(String[] args) throws Exception {
        Path input = args.length > 0 ? Path.of(args[0]) : INPUT;
        Path outDir = Path.of(System.getProperty("user.home"),
                ".whiteowl", "discovery", "crudeoil", "development");
        DiscoveryConfig config = DiscoveryConfig.mcxCrude(input, outDir);

        Bars bars = CsvBarsLoader.load(input, config.barMinutes());
        var arrays = bars.arrays();
        int exitMinute = config.exitTime().getHour() * 60 + config.exitTime().getMinute();
        SessionIndex session = new SessionIndex(arrays, config.barMinutes(), exitMinute);
        SignalContext ctx = new SignalContext(arrays, session);
        FeatureMatrix features = new FeatureMatrix(ctx, config.lookbacks(), exitMinute);
        log.info("Data ready: {} days, {} bars, {} features", session.dayCount(), ctx.size, features.columnCount());

        EntrySignal signal = SignalCatalog.all(ctx).stream()
                .filter(s -> s.name().equals(SIGNAL)).findFirst().orElseThrow();
        byte[] sig = signal.evaluate(ctx);

        StringBuilder report = new StringBuilder();

        // ── Stage 1: MFE/MAE excursion analysis (EOD exits) ──────────────
        DiscoveryEngine eodEngine = new DiscoveryEngine(ctx, config);
        List<DiscoveryTrade> eodTrades = eodEngine.run(sig);
        log.info("{}: {} EOD-exit trades", SIGNAL, eodTrades.size());
        report.append(ExcursionAnalyzer.report(eodTrades)).append('\n');

        // ── Stage 2: stop x target grid ──────────────────────────────────
        StopTargetGrid grid = new StopTargetGrid(ctx, config);
        List<StopTargetGrid.Cell> cells = grid.sweep(sig, STOPS, TARGETS);
        report.append(StopTargetGrid.renderGrid(cells, STOPS, TARGETS, 12)).append('\n');

        // Pick the best cell with both halves profitable (stability gate);
        // fall back to the top cell if none qualifies.
        StopTargetGrid.Cell best = cells.stream()
                .filter(c -> c.sortinoH1() > 0 && c.sortinoH2() > 0)
                .findFirst().orElse(cells.get(0));
        Double bestStop = best.stop() == 0 ? null : best.stop();
        Double bestTarget = best.target() == 0 ? null : best.target();
        report.append(String.format("CHOSEN: stop=%s ATR, target=%s ATR%n%n",
                bestStop, bestTarget));
        log.info("Chosen params: stop={} ATR, target={} ATR", bestStop, bestTarget);

        // ── Stage 3: re-run with stops, write enhanced CSV ───────────────
        DiscoveryConfig enhanced = config.withStops(bestStop, bestTarget);
        DiscoveryEngine enhEngine = new DiscoveryEngine(ctx, enhanced);
        List<DiscoveryTrade> trades = enhEngine.run(sig);
        log.info("Enhanced strategy: {} trades", trades.size());
        Path tradeCsv = outDir.resolve(SIGNAL + "_enhanced.csv");
        TradeCsvWriter.write(tradeCsv, SIGNAL, trades, features);
        report.append(String.format("Enhanced trades: %d -> %s%n%n", trades.size(), tradeCsv));

        // ── Stage 4: TOD / DOW breakdown ─────────────────────────────────
        report.append(BreakdownAnalyzer.report(trades, 60)).append('\n');

        // ── Stage 5: meta-labeling on enhanced trades ────────────────────
        MetaLabeler labeler = new MetaLabeler();
        MetaLabeler.MetaReport meta = labeler.run(SIGNAL + "_enhanced", trades,
                features, outDir.resolve("trees"));
        report.append(String.format(
                "META-LABELING (enhanced trades)%nbase_sortino=%.3f  oos_sortino=%.3f  "
                        + "oos_win=%.1f%%  oos_avg=%.2f  oos_total=%.0f  selected=%d  acc=%.1f%%%n",
                meta.baseSortino(), meta.oosSortino(), meta.oosWinRate() * 100,
                meta.oosAvgNet(), meta.oosTotalNet(), meta.oosSelected(),
                meta.oosAccuracy() * 100));
        if (!meta.topFeatures().isEmpty()) {
            report.append("features: ").append(String.join(", ", meta.topFeatures())).append('\n');
        }
        for (String rule : meta.topRules()) {
            report.append("rule: ").append(rule).append('\n');
        }

        Path reportFile = outDir.resolve("development_report.txt");
        Files.createDirectories(reportFile.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(reportFile)) {
            w.write(report.toString());
        }
        log.info("\n{}", report);
        log.info("Report written to {}", reportFile);
    }
}
