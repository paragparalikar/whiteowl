package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.metrics.MetricsCalculator;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.scripting.script.ScriptCompilationException;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

/**
 * Supertrend parameter optimization driver for NIFTY 50.
 *
 * Sweeps period and multiplier across 8 timeframes (1m, 2m, 3m, 5m, 10m, 15m, 30m, 1h)
 * and produces a robustness analysis to find the most stable parameter combination.
 */
public final class SupertrendOptimizationDriver {

    private static final Logger log = LoggerFactory.getLogger(SupertrendOptimizationDriver.class);

    private static final String SCRIP_ID = "NSE:NIFTY 50";
    private static final float INITIAL_CAPITAL = 1_000_000f;
    private static final float COST_PERCENT = 0.03f;
    private static final float SLIPPAGE_PERCENT = 0.02f;
    private static final float VOLUME_PARTICIPATION = 100f;

    private static final int[] PERIODS = {7, 10, 12, 14, 20, 25, 30};
    private static final float[] MULTIPLIERS = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f};

    private static final Timeframe[] TIMEFRAMES = {
            Timeframe.ONE_MINUTE,
            Timeframe.THREE_MINUTE,
            Timeframe.FIVE_MINUTE,
            Timeframe.TEN_MINUTE,
            Timeframe.FIFTEEN_MINUTE,
            Timeframe.THIRTY_MINUTE,
            Timeframe.ONE_HOUR
    };

    private static final String STRATEGY_SCRIPT = """
            import groovy.transform.Field

            @Field def st
            @Field def dir
            @Field int period
            @Field float mult
            @Field int qty = 50

            void setup() {
                period = input("Period", 10, 3, 50, 1) as int
                mult = input("Multiplier", 3.0, 1.0, 6.0, 0.5) as float
                def result = supertrend(period, mult)
                st = result[0]
                dir = result[1]
            }

            void onBar(int bar, String scripId) {
                if (bar < period + 2) return

                float currentDir = dir[0]
                float prevDir = dir[-1]

                if (currentDir > 0 && prevDir <= 0) {
                    if (hasOpenPositions()) shortExit()
                    longEntry(qty)
                }
                else if (currentDir < 0 && prevDir >= 0) {
                    if (hasOpenPositions()) longExit()
                    shortEntry(qty)
                }
            }
            """;

    record OptResult(
            Timeframe timeframe,
            int period,
            float multiplier,
            int totalTrades,
            float netProfitPercent,
            float sharpeRatio,
            float sortinoRatio,
            float maxDrawdown,
            float profitFactor,
            float winRate,
            float cagr,
            float calmarRatio
    ) {}

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Supertrend Parameter Optimization — NIFTY 50");
        log.info("  Capital: ₹{:.0f}  |  Periods: {}  |  Multipliers: {}  |  Timeframes: {}",
                INITIAL_CAPITAL, PERIODS.length, MULTIPLIERS.length, TIMEFRAMES.length);
        log.info("  Total combinations: {}", PERIODS.length * MULTIPLIERS.length * TIMEFRAMES.length);
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        BarsRepository barsRepo = new FileBarsRepository();
        Class<? extends Script> compiledClass = ScriptCompiler.compileClass(
                STRATEGY_SCRIPT, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);

        List<OptResult> allResults = Collections.synchronizedList(new ArrayList<>());
        int totalCombinations = PERIODS.length * MULTIPLIERS.length * TIMEFRAMES.length;
        int completed = 0;

        for (Timeframe tf : TIMEFRAMES) {
            log.info("═══ Loading {} bars for {} ═══", tf.getDisplayLabel(), SCRIP_ID);
            Bars bars;
            try {
                bars = barsRepo.load(SCRIP_ID, tf);
            } catch (IOException e) {
                log.warn("  No data for {} — skipping: {}", tf.getDisplayLabel(), e.getMessage());
                completed += PERIODS.length * MULTIPLIERS.length;
                continue;
            }
            if (bars == null || bars.size() < 100) {
                log.warn("  Insufficient data for {} ({} bars) — skipping",
                        tf.getDisplayLabel(), bars != null ? bars.size() : 0);
                completed += PERIODS.length * MULTIPLIERS.length;
                continue;
            }
            log.info("  Loaded {} bars", bars.size());
            BarsArrays arrays = bars.arrays();

            for (int period : PERIODS) {
                for (float mult : MULTIPLIERS) {
                    try {
                        OptResult result = runBacktest(compiledClass, arrays, tf, period, mult);
                        if (result != null) {
                            allResults.add(result);
                        }
                    } catch (Exception e) {
                        log.debug("  Error P={} M={} TF={}: {}", period, mult, tf.getLabel(), e.getMessage());
                    }
                    completed++;
                    if (completed % 20 == 0) {
                        log.info("  Progress: {}/{}", completed, totalCombinations);
                    }
                }
            }
        }

        log.info("");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  OPTIMIZATION COMPLETE — {} valid results", allResults.size());
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // ── Per-Timeframe Best ────────────────────────────────────────
        printPerTimeframeBest(allResults);

        // ── Cross-Timeframe Robustness Analysis ──────────────────────
        printRobustnessAnalysis(allResults);

        // ── Generate CSV report ──────────────────────────────────────
        Path reportPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports",
                "supertrend_optimization.csv");
        reportPath.getParent().toFile().mkdirs();
        writeCsvReport(reportPath, allResults);
        log.info("CSV report: {}", reportPath.toAbsolutePath());

        // ── Generate HTML heatmap report ─────────────────────────────
        Path htmlPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports",
                "supertrend_optimization.html");
        writeHtmlReport(htmlPath, allResults);
        log.info("HTML report: {}", htmlPath.toAbsolutePath());
    }

    // ══════════════════════════════════════════════════════════════════
    //  Backtest runner
    // ══════════════════════════════════════════════════════════════════

    private static OptResult runBacktest(Class<? extends Script> compiledClass,
                                          BarsArrays arrays, Timeframe tf,
                                          int period, float mult) throws Exception {
        Script instance = ScriptCompiler.instantiate(compiledClass);
        if (!(instance instanceof TradingStrategyBase strategy)) return null;

        Map<String, Number> inputs = new LinkedHashMap<>();
        inputs.put("Period", period);
        inputs.put("Multiplier", mult);
        strategy.setInputOverrides(inputs);

        BacktestEngine engine = new BacktestEngine(
                INITIAL_CAPITAL, COST_PERCENT, SLIPPAGE_PERCENT, VOLUME_PARTICIPATION, null);

        ScripResult scripResult = engine.run(SCRIP_ID, arrays, strategy);

        BacktestConfig config = BacktestConfig.builder()
                .timeframe(tf)
                .initialCapital(INITIAL_CAPITAL)
                .costPercent(COST_PERCENT)
                .slippagePercent(SLIPPAGE_PERCENT)
                .build();

        BacktestReport report = MetricsCalculator.compute(List.of(scripResult), config);

        return new OptResult(
                tf, period, mult,
                report.getTotalTrades(),
                report.getNetProfitPercent(),
                report.getSharpeRatio(),
                report.getSortinoRatio(),
                report.getMaxDrawdown(),
                report.getProfitFactor(),
                report.getWinRate(),
                report.getCagr(),
                report.getCalmarRatio()
        );
    }

    // ══════════════════════════════════════════════════════════════════
    //  Analysis
    // ══════════════════════════════════════════════════════════════════

    private static void printPerTimeframeBest(List<OptResult> results) {
        log.info("═══ PER-TIMEFRAME BEST (by Sharpe) ═══");
        log.info("  {:<12} {:>6} {:>6} {:>8} {:>8} {:>8} {:>8} {:>8} {:>6}",
                "Timeframe", "Period", "Mult", "Sharpe", "Sortino", "MaxDD%", "PF", "CAGR%", "Trades");
        log.info("  {}", "─".repeat(82));

        for (Timeframe tf : TIMEFRAMES) {
            results.stream()
                    .filter(r -> r.timeframe == tf && r.totalTrades >= 10)
                    .max(Comparator.comparingDouble(r -> score(r)))
                    .ifPresent(best -> log.info("  {:<12} {:>6} {:>6.1f} {:>8.2f} {:>8.2f} {:>7.1f}% {:>8.2f} {:>7.1f}% {:>6}",
                            tf.getDisplayLabel(), best.period, best.multiplier,
                            best.sharpeRatio, best.sortinoRatio, best.maxDrawdown,
                            best.profitFactor, best.cagr, best.totalTrades));
        }
        log.info("");
    }

    private static void printRobustnessAnalysis(List<OptResult> results) {
        log.info("═══ CROSS-TIMEFRAME ROBUSTNESS ANALYSIS ═══");
        log.info("");

        // For each (period, multiplier) pair, compute a "robustness score"
        // = average normalized Sharpe across all timeframes where it's profitable
        Map<String, List<OptResult>> byParams = new LinkedHashMap<>();
        for (OptResult r : results) {
            String key = r.period + ":" + r.multiplier;
            byParams.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        record RobustnessScore(int period, float multiplier, double avgSharpe,
                               double avgSortino, double avgMaxDD, double avgPF,
                               int profitableTFs, int totalTFs, double stabilityScore) {}

        List<RobustnessScore> scores = new ArrayList<>();
        for (Map.Entry<String, List<OptResult>> entry : byParams.entrySet()) {
            List<OptResult> paramResults = entry.getValue();
            String[] parts = entry.getKey().split(":");
            int period = Integer.parseInt(parts[0]);
            float mult = Float.parseFloat(parts[1]);

            List<OptResult> valid = paramResults.stream()
                    .filter(r -> r.totalTrades >= 10)
                    .toList();
            if (valid.isEmpty()) continue;

            double avgSharpe = valid.stream().mapToDouble(r -> r.sharpeRatio).average().orElse(0);
            double avgSortino = valid.stream().mapToDouble(r -> r.sortinoRatio).average().orElse(0);
            double avgMaxDD = valid.stream().mapToDouble(r -> r.maxDrawdown).average().orElse(0);
            double avgPF = valid.stream().mapToDouble(r -> Math.min(r.profitFactor, 10f)).average().orElse(0);
            int profitableTFs = (int) valid.stream().filter(r -> r.netProfitPercent > 0).count();

            // Stability: low variance of Sharpe across timeframes
            double sharpeVariance = 0;
            for (OptResult r : valid) {
                sharpeVariance += (r.sharpeRatio - avgSharpe) * (r.sharpeRatio - avgSharpe);
            }
            sharpeVariance = valid.size() > 1 ? sharpeVariance / (valid.size() - 1) : 0;
            double sharpeStdDev = Math.sqrt(sharpeVariance);

            // Robustness = avgSharpe * timeframe coverage * stability penalty
            double coverage = (double) profitableTFs / TIMEFRAMES.length;
            double stabilityPenalty = sharpeStdDev > 0 ? 1.0 / (1.0 + sharpeStdDev) : 1.0;
            double ddPenalty = avgMaxDD < -30 ? 0.5 : (avgMaxDD < -20 ? 0.75 : 1.0);
            double stabilityScore = avgSharpe * coverage * stabilityPenalty * ddPenalty;

            scores.add(new RobustnessScore(period, mult, avgSharpe, avgSortino,
                    avgMaxDD, avgPF, profitableTFs, valid.size(), stabilityScore));
        }

        scores.sort(Comparator.comparingDouble(RobustnessScore::stabilityScore).reversed());

        log.info("  Top 15 most ROBUST parameter combinations:");
        log.info("  {:<6} {:>6} {:>10} {:>9} {:>9} {:>8} {:>8} {:>12}",
                "Period", "Mult", "Stability", "AvgSharpe", "AvgSortino", "AvgMaxDD", "AvgPF", "Profitable");
        log.info("  {}", "─".repeat(78));

        for (int i = 0; i < Math.min(15, scores.size()); i++) {
            RobustnessScore s = scores.get(i);
            log.info("  {:>6} {:>6.1f} {:>10.4f} {:>9.2f} {:>9.2f} {:>7.1f}% {:>8.2f} {:>5}/{:<6}",
                    s.period, s.multiplier, s.stabilityScore, s.avgSharpe, s.avgSortino,
                    s.avgMaxDD, s.avgPF, s.profitableTFs, s.totalTFs + " TFs");
        }
        log.info("");

        if (!scores.isEmpty()) {
            RobustnessScore best = scores.get(0);
            log.info("  ╔══════════════════════════════════════════════════════════════╗");
            log.info("  ║  RECOMMENDED ROBUST PARAMETERS                              ║");
            log.info("  ║  Period     : {:>6}                                         ║", best.period);
            log.info("  ║  Multiplier : {:>6.1f}                                         ║", best.multiplier);
            log.info("  ║  Stability  : {:>10.4f}                                    ║", best.stabilityScore);
            log.info("  ║  Avg Sharpe : {:>9.2f}                                      ║", best.avgSharpe);
            log.info("  ║  Profitable : {}/{} timeframes                                ║",
                    best.profitableTFs, best.totalTFs);
            log.info("  ╚══════════════════════════════════════════════════════════════╝");
        }

        // ── Period stability analysis ─────────────────────────────────
        log.info("");
        log.info("  PERIOD STABILITY (averaged across all multipliers):");
        log.info("  {:<8} {:>9} {:>9} {:>8}",
                "Period", "AvgSharpe", "AvgMaxDD", "AvgPF");
        log.info("  {}", "─".repeat(40));
        for (int period : PERIODS) {
            List<OptResult> forPeriod = results.stream()
                    .filter(r -> r.period == period && r.totalTrades >= 10).toList();
            if (forPeriod.isEmpty()) continue;
            double avgSh = forPeriod.stream().mapToDouble(r -> r.sharpeRatio).average().orElse(0);
            double avgDD = forPeriod.stream().mapToDouble(r -> r.maxDrawdown).average().orElse(0);
            double avgPF = forPeriod.stream().mapToDouble(r -> Math.min(r.profitFactor, 10)).average().orElse(0);
            log.info("  {:>8} {:>9.2f} {:>8.1f}% {:>8.2f}", period, avgSh, avgDD, avgPF);
        }

        // ── Multiplier stability analysis ────────────────────────────
        log.info("");
        log.info("  MULTIPLIER STABILITY (averaged across all periods):");
        log.info("  {:<8} {:>9} {:>9} {:>8}",
                "Mult", "AvgSharpe", "AvgMaxDD", "AvgPF");
        log.info("  {}", "─".repeat(40));
        for (float mult : MULTIPLIERS) {
            List<OptResult> forMult = results.stream()
                    .filter(r -> r.multiplier == mult && r.totalTrades >= 10).toList();
            if (forMult.isEmpty()) continue;
            double avgSh = forMult.stream().mapToDouble(r -> r.sharpeRatio).average().orElse(0);
            double avgDD = forMult.stream().mapToDouble(r -> r.maxDrawdown).average().orElse(0);
            double avgPF = forMult.stream().mapToDouble(r -> Math.min(r.profitFactor, 10)).average().orElse(0);
            log.info("  {:>8.1f} {:>9.2f} {:>8.1f}% {:>8.2f}", mult, avgSh, avgDD, avgPF);
        }
        log.info("");
    }

    private static double score(OptResult r) {
        if (r.totalTrades < 10) return Double.NEGATIVE_INFINITY;
        double s = r.sharpeRatio + 0.1 * r.sortinoRatio;
        if (r.maxDrawdown < -30) s -= 1.0;
        if (r.maxDrawdown < -20) s -= 0.5;
        return s;
    }

    // ══════════════════════════════════════════════════════════════════
    //  Report writers
    // ══════════════════════════════════════════════════════════════════

    private static void writeCsvReport(Path path, List<OptResult> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("Timeframe,Period,Multiplier,Trades,NetProfit%,Sharpe,Sortino,MaxDD%,PF,WinRate%,CAGR%,Calmar\n");
            for (OptResult r : results) {
                w.write(String.format(Locale.US, "%s,%d,%.1f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        r.timeframe.getDisplayLabel(), r.period, r.multiplier,
                        r.totalTrades, r.netProfitPercent, r.sharpeRatio, r.sortinoRatio,
                        r.maxDrawdown, r.profitFactor, r.winRate, r.cagr, r.calmarRatio));
            }
        }
    }

    private static void writeHtmlReport(Path path, List<OptResult> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <title>Supertrend Optimization — NIFTY 50</title>
                    <style>
                    body { font-family: 'Segoe UI', sans-serif; background: #1a1a2e; color: #e0e0e0; margin: 20px; }
                    h1 { color: #00d4ff; }
                    h2 { color: #ff6b6b; margin-top: 30px; }
                    table { border-collapse: collapse; margin: 10px 0; font-size: 13px; }
                    th { background: #16213e; color: #00d4ff; padding: 8px 12px; text-align: right; }
                    td { padding: 6px 12px; text-align: right; border-bottom: 1px solid #2a2a4a; }
                    tr:hover { background: #16213e; }
                    .positive { color: #4ade80; }
                    .negative { color: #f87171; }
                    .heatmap { display: inline-block; margin: 10px; }
                    .best { background: #1a3a2a !important; font-weight: bold; }
                    </style>
                    </head>
                    <body>
                    <h1>Supertrend Parameter Optimization — NIFTY 50</h1>
                    """);

            // Per-timeframe heatmaps
            for (Timeframe tf : TIMEFRAMES) {
                List<OptResult> tfResults = results.stream()
                        .filter(r -> r.timeframe == tf).toList();
                if (tfResults.isEmpty()) continue;

                OptResult tfBest = tfResults.stream()
                        .filter(r -> r.totalTrades >= 10)
                        .max(Comparator.comparingDouble(r -> score(r)))
                        .orElse(null);

                w.write("<h2>" + tf.getDisplayLabel() + " — Sharpe Ratio Heatmap</h2>\n");
                w.write("<table><tr><th>Period \\ Mult</th>");
                for (float m : MULTIPLIERS) {
                    w.write("<th>" + String.format("%.1f", m) + "</th>");
                }
                w.write("</tr>\n");

                for (int p : PERIODS) {
                    w.write("<tr><th>" + p + "</th>");
                    for (float m : MULTIPLIERS) {
                        final int fp = p;
                        final float fm = m;
                        OptResult cell = tfResults.stream()
                                .filter(r -> r.period == fp && r.multiplier == fm)
                                .findFirst().orElse(null);
                        if (cell != null) {
                            boolean isBest = tfBest != null && cell.period == tfBest.period
                                    && cell.multiplier == tfBest.multiplier;
                            String cls = cell.sharpeRatio > 0 ? "positive" : "negative";
                            if (isBest) cls += " best";
                            w.write(String.format("<td class='%s'>%.2f</td>", cls, cell.sharpeRatio));
                        } else {
                            w.write("<td>—</td>");
                        }
                    }
                    w.write("</tr>\n");
                }
                w.write("</table>\n");
            }

            // Summary table
            w.write("<h2>All Results</h2>\n");
            w.write("<table><tr><th>Timeframe</th><th>Period</th><th>Mult</th><th>Trades</th>");
            w.write("<th>NetProfit%</th><th>Sharpe</th><th>Sortino</th><th>MaxDD%</th>");
            w.write("<th>PF</th><th>WinRate%</th><th>CAGR%</th></tr>\n");

            List<OptResult> sorted = new ArrayList<>(results);
            sorted.sort(Comparator.comparingDouble(r -> -score(r)));

            for (OptResult r : sorted) {
                String profitCls = r.netProfitPercent > 0 ? "positive" : "negative";
                w.write(String.format(Locale.US,
                        "<tr><td>%s</td><td>%d</td><td>%.1f</td><td>%d</td>" +
                                "<td class='%s'>%.2f</td><td class='%s'>%.2f</td>" +
                                "<td class='%s'>%.2f</td><td class='negative'>%.1f</td>" +
                                "<td>%.2f</td><td>%.1f</td><td class='%s'>%.1f</td></tr>\n",
                        r.timeframe.getDisplayLabel(), r.period, r.multiplier, r.totalTrades,
                        profitCls, r.netProfitPercent,
                        r.sharpeRatio > 0 ? "positive" : "negative", r.sharpeRatio,
                        r.sortinoRatio > 0 ? "positive" : "negative", r.sortinoRatio,
                        r.maxDrawdown,
                        r.profitFactor,
                        r.winRate,
                        r.cagr > 0 ? "positive" : "negative", r.cagr));
            }
            w.write("</table>\n</body>\n</html>\n");
        }
    }

}
