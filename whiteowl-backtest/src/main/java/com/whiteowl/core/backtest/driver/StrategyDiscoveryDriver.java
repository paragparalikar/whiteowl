package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.discovery.*;
import com.whiteowl.core.bar.model.Bars;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Strategy-discovery pipeline for MCX crude continuous futures:
 *
 * <ol>
 *   <li>Load the 1-min continuous CSV, resample to the configured bar size.</li>
 *   <li>Build session index and the regime-feature matrix (indicator x
 *       Fibonacci lookbacks, ATR-normalized).</li>
 *   <li>For each of the 25 entry signals, simulate next-open entries with
 *       fixed-cost EOD exits and write a per-signal feature CSV.</li>
 *   <li>Run meta-labeling (purged walk-forward decision tree) per signal and
 *       rank candidates by out-of-sample Sortino.</li>
 * </ol>
 */
@Slf4j
public final class StrategyDiscoveryDriver {

    private static final Path INPUT = Path.of(
            "C:\\trading\\OptionsData_Order91_GOLD-GOLDM-and-6-more_Futures_2023-2026",
            "continuous_csv", "CRUDEOIL_continuous_1min.csv");

    public static void main(String[] args) throws Exception {
        Path input = args.length > 0 ? Path.of(args[0]) : INPUT;
        Path outDir = Path.of(System.getProperty("user.home"),
                ".whiteowl", "discovery", "crudeoil");
        DiscoveryConfig config = DiscoveryConfig.mcxCrude(input, outDir);

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  Strategy Discovery — {}", input.getFileName());
        log.info("══════════════════════════════════════════════════════════════");

        // ── Load + resample ──────────────────────────────────────────────
        Bars bars = CsvBarsLoader.load(input, config.barMinutes());
        var arrays = bars.arrays();

        // SessionIndex iterates raw arrays — it is safe because it bounds
        // every loop by arrays.size(), not array length.
        int exitMinute = config.exitTime().getHour() * 60 + config.exitTime().getMinute();
        SessionIndex session = new SessionIndex(arrays, config.barMinutes(), exitMinute);
        SignalContext ctx = new SignalContext(arrays, session);
        log.info("{} days, {} bars", session.dayCount(), ctx.size);

        // ── Feature matrix ───────────────────────────────────────────────
        log.info("Building feature matrix ({} lookbacks)...", config.lookbacks().length);
        FeatureMatrix features = new FeatureMatrix(ctx, config.lookbacks(), exitMinute);
        log.info("Feature matrix: {} columns", features.columnCount());

        // ── Per-signal simulation + CSV + meta-labeling ──────────────────
        Path csvDir = outDir.resolve("trades");
        Path dotDir = outDir.resolve("trees");
        DiscoveryEngine engine = new DiscoveryEngine(ctx, config);
        MetaLabeler labeler = new MetaLabeler();
        List<MetaLabeler.MetaReport> reports = new ArrayList<>();

        List<EntrySignal> signals = SignalCatalog.all(ctx);
        log.info("Evaluating {} signals", signals.size());
        for (EntrySignal signal : signals) {
            byte[] sig = signal.evaluate(ctx);
            List<DiscoveryTrade> trades = engine.run(sig);
            log.info("── {}: {} trades", signal.name(), trades.size());
            if (trades.isEmpty()) continue;
            TradeCsvWriter.write(csvDir.resolve(signal.name() + ".csv"),
                    signal.name(), trades, features);
            reports.add(labeler.run(signal.name(), trades, features, dotDir));
        }

        // ── Summary report ───────────────────────────────────────────────
        reports.sort(Comparator.comparingDouble(r ->
                Double.isNaN(r.oosSortino()) ? Double.NEGATIVE_INFINITY : -r.oosSortino()));
        Path summaryTxt = outDir.resolve("summary.txt");
        Files.createDirectories(summaryTxt.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(summaryTxt)) {
            w.write(renderSummary(reports));
        }
        log.info("{}", renderSummary(reports));
        Path summary = outDir.resolve("summary.csv");
        Files.createDirectories(summary.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(summary)) {
            w.write("signal,trades,usable,base_sortino,oos_sortino,oos_win_rate,"
                    + "oos_avg_net,oos_total_net,oos_selected,oos_accuracy,top_features");
            w.newLine();
            for (var r : reports) {
                w.write(String.format("%s,%d,%d,%.3f,%.3f,%.3f,%.2f,%.0f,%d,%.3f,\"%s\"%n",
                        r.signalName(), r.totalTrades(), r.usableTrades(),
                        r.baseSortino(), r.oosSortino(), r.oosWinRate(),
                        r.oosAvgNet(), r.oosTotalNet(), r.oosSelected(),
                        r.oosAccuracy(), String.join("; ", r.topFeatures())));
            }
        }
        log.info("Summary written to {}", summary);
        log.info("Trade CSVs in {}", csvDir);
        log.info("Tree dumps in {}", dotDir);
    }

    private static String renderSummary(List<MetaLabeler.MetaReport> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("META-LABELING RESULTS (OOS = purged walk-forward, filtered = tree predicts win)\n");
        sb.append(String.format("%-18s %7s %6s %9s %9s %8s %9s %10s %8s %8s%n",
                "signal", "trades", "usable", "baseSort", "oosSort", "oosWin%", "oosAvgNet", "oosTotNet", "sel", "acc"));
        for (var r : reports) {
            sb.append(String.format("%-18s %7d %6d %9s %9s %8s %9s %10s %8d %8s%n",
                    r.signalName(), r.totalTrades(), r.usableTrades(),
                    f(r.baseSortino()), f(r.oosSortino()),
                    pct(r.oosWinRate()), f(r.oosAvgNet()), f(r.oosTotalNet()),
                    r.oosSelected(), pct(r.oosAccuracy())));
            if (!r.topFeatures().isEmpty()) {
                sb.append("    features: ").append(String.join(", ", r.topFeatures())).append('\n');
            }
            for (String rule : r.topRules()) {
                sb.append("    rule: ").append(rule).append('\n');
            }
        }
        return sb.toString();
    }

    private static String f(double v) {
        return Double.isNaN(v) ? "-" : String.format("%.3f", v);
    }

    private static String pct(double v) {
        return Double.isNaN(v) ? "-" : String.format("%.1f%%", v * 100);
    }
}
