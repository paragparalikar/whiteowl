package com.whiteowl.core.backtest.v2.optimization.report;

import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;
import com.whiteowl.core.backtest.v2.optimization.TradeRun;
import com.whiteowl.core.backtest.v2.optimization.analysis.BucketStats;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis;
import com.whiteowl.core.backtest.v2.optimization.analysis.RMultipleAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.StreakAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.exit.Excursion;
import com.whiteowl.core.backtest.v2.optimization.exit.ExcursionAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.plateau.ParameterSelection;
import com.whiteowl.core.backtest.v2.optimization.plateau.PlateauResult;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.ParameterDriftStats;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardWindowResult;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.backtest.v2.optimization.report.Svg.esc;
import static com.whiteowl.core.backtest.v2.optimization.report.Svg.fmt;

/**
 * Phase 9 — self-contained HTML report generated from the
 * {@link OptimizationContext} alone. All CSS is inline and all charts are
 * inline SVG — no external server or assets needed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HtmlReportGenerator {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Kolkata"));
    private static final int TOP_ROWS = 15;

    public static Path write(OptimizationContext ctx, Path output) {
        try {
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, generate(ctx), StandardCharsets.UTF_8);
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String generate(OptimizationContext ctx) {
        StringBuilder h = new StringBuilder(64_000);
        h.append("""
                <!DOCTYPE html><html><head><meta charset='utf-8'>
                <title>Optimization Report</title><style>
                body{font-family:'Segoe UI',Arial,sans-serif;background:#0f1420;color:#dbe2ef;
                     margin:0;padding:24px;line-height:1.45}
                h1{font-size:22px;color:#fff;border-bottom:2px solid #2e6bd6;padding-bottom:8px}
                h2{font-size:17px;color:#7fb0ff;margin:28px 0 10px}
                h3{font-size:14px;color:#9db8dd;margin:18px 0 6px}
                table{border-collapse:collapse;width:100%;font-size:12px;margin:8px 0}
                th{background:#1c2740;color:#8fb5f0;text-align:left;padding:6px 8px;
                   border-bottom:1px solid #2e3c5c}
                td{padding:5px 8px;border-bottom:1px solid #1e2a45}
                tr:nth-child(even) td{background:#141c30}
                .num{text-align:right;font-variant-numeric:tabular-nums}
                .card{background:#162032;border:1px solid #26344f;border-radius:8px;
                      padding:14px 18px;margin:10px 0}
                .grid{display:flex;flex-wrap:wrap;gap:10px}
                .kpi{background:#162032;border:1px solid #26344f;border-radius:8px;
                     padding:10px 16px;min-width:130px}
                .kpi .v{font-size:18px;color:#fff;font-weight:600}
                .kpi .k{font-size:11px;color:#8fa3c8;text-transform:uppercase}
                .warn{background:#3a2a12;border:1px solid #7a5a1e;color:#ffcf7a;
                      padding:4px 10px;border-radius:4px;display:inline-block;margin:2px}
                .ok{color:#7bd88f}.bad{color:#ff7a7a}
                .chart{width:100%%;max-width:700px;background:#101827;border-radius:6px;
                       margin:8px 0}
                .chart .axis{stroke:#3a4a6b;stroke-width:1}
                .chart .line{fill:none;stroke:#5aa2ff;stroke-width:1.5}
                .chart .pt{fill:#8fc1ff}
                .chart .plateau{fill:#2e6bd6;opacity:.22}
                .chart .is{fill:#5aa2ff}.chart .oos{fill:#f0a35e}
                .chart .ddLine{fill:none;stroke:#ff7a7a;stroke-width:1.2}
                .chart .ddArea{fill:#ff7a7a;opacity:.15}
                .chart .title{fill:#dbe2ef;font-size:13px}
                .chart .lbl{fill:#8fa3c8;font-size:10px}
                .chart .oosT{fill:#f0a35e}
                .mono{font-family:Consolas,monospace;font-size:12px;color:#9fd0a8}
                .reason{color:#8fa3c8;font-size:11px;font-style:italic}
                </style></head><body>
                """);
        h.append("<h1>Strategy Optimization Report</h1>");
        executiveSummary(h, ctx);
        phase1(h, ctx);
        phase2(h, ctx);
        phase3(h, ctx);
        phase4(h, ctx);
        phase5(h, ctx);
        phase6(h, ctx);
        phase7(h, ctx);
        phase8(h, ctx);
        warnings(h, ctx);
        audit(h, ctx);
        h.append("</body></html>");
        return h.toString();
    }

    // ── Sections ─────────────────────────────────────────────────────────

    private static void executiveSummary(StringBuilder h, OptimizationContext ctx) {
        h.append("<h2>Executive Summary</h2><div class='grid'>");
        kpi(h, "Strategy", esc(ctx.getStrategyId()));
        kpi(h, "Instruments", esc(String.join(", ", ctx.getScripIds())));
        kpi(h, "Timeframes", ctx.getTimeframes().size() + "");
        kpi(h, "Combinations tested", String.valueOf(ctx.getOptimizationResults().size()));
        kpi(h, "Job", esc(ctx.getJob().getId().substring(0, 8)));
        kpi(h, "Created", TS_FMT.format(ctx.getJob().getCreatedAt()));
        h.append("</div>");
        TradeRun run = ctx.getFinalTradeRun();
        if (run != null && run.equityCurve() != null && run.equityCurve().getSize() > 1) {
            h.append("<div class='card'><h3>Final configuration — equity &amp; drawdown</h3>");
            h.append(Svg.curveChart(run.equityCurve().getValues(), "Equity curve", false));
            h.append(Svg.curveChart(Svg.drawdownSeries(run.equityCurve().getValues()),
                    "Drawdown %", true));
            h.append("</div>");
        }
    }

    private static void phase1(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getOptimizationResults().isEmpty()) return;
        h.append("<h2>Phase 1 — Parameter Grid Evaluation</h2>");
        for (Timeframe tf : ctx.getTimeframes()) {
            List<OptimizationResult> rs = ctx.resultsFor(tf);
            if (rs.isEmpty()) continue;
            long ok = rs.stream()
                    .filter(r -> r.status() == OptimizationResult.Status.OK).count();
            long thin = rs.stream()
                    .filter(r -> r.status() == OptimizationResult.Status.INSUFFICIENT_SAMPLE).count();
            long failed = rs.size() - ok - thin;
            h.append(String.format("<h3>%s — %d combinations (%d ok, %d thin, %d failed)</h3>",
                    tf.getCode(), rs.size(), ok, thin, failed));
            h.append("<table><tr><th>Combination</th><th class='num'>Sortino</th>"
                    + "<th class='num'>Sharpe</th><th class='num'>CAGR%</th>"
                    + "<th class='num'>MaxDD%</th><th class='num'>Trades</th>"
                    + "<th class='num'>WinRate%</th><th class='num'>Expectancy%</th>"
                    + "<th class='num'>Payoff</th><th>Status</th></tr>");
            rs.stream()
                    .filter(r -> r.metrics() != null)
                    .sorted(Comparator.comparingDouble(
                            (OptimizationResult r) -> r.metrics().sortinoRatio()).reversed())
                    .limit(TOP_ROWS)
                    .forEach(r -> {
                        OptimizationMetrics m = r.metrics();
                        h.append(String.format(
                                "<tr><td class='mono'>%s</td><td class='num'>%.2f</td>"
                                        + "<td class='num'>%.2f</td><td class='num'>%.2f</td>"
                                        + "<td class='num'>%.2f</td><td class='num'>%d</td>"
                                        + "<td class='num'>%.1f</td><td class='num'>%.2f</td>"
                                        + "<td class='num'>%s</td><td>%s</td></tr>",
                                esc(r.combination().id()), m.sortinoRatio(), m.sharpeRatio(),
                                m.cagr(), m.maxDrawdown(), m.totalTrades(), m.winRate(),
                                m.expectancy(), finite(m.payoffRatio()), r.status()));
                    });
            h.append("</table>");
            h.append(paramHeatmap(rs));
        }
    }

    /** Sortino heatmap over the two swept parameters (cols × rows). */
    private static String paramHeatmap(List<OptimizationResult> rs) {
        // Swept params = keys with >1 distinct value across combinations.
        Map<String, java.util.Set<Double>> distinct = new java.util.LinkedHashMap<>();
        for (OptimizationResult r : rs) {
            r.combination().values().forEach((k, v) ->
                    distinct.computeIfAbsent(k, x -> new java.util.TreeSet<>())
                            .add(v.doubleValue()));
        }
        List<String> swept = distinct.entrySet().stream()
                .filter(e -> e.getValue().size() > 1).map(Map.Entry::getKey).toList();
        if (swept.size() < 2) return "";
        String xParam = swept.get(0), yParam = swept.get(1);
        List<Double> xs = List.copyOf(distinct.get(xParam));
        List<Double> ys = List.copyOf(distinct.get(yParam));
        double[][] grid = new double[ys.size()][xs.size()];
        for (double[] row : grid) java.util.Arrays.fill(row, Double.NaN);
        for (OptimizationResult r : rs) {
            int xi = xs.indexOf(r.combination().values().get(xParam).doubleValue());
            int yi = ys.indexOf(r.combination().values().get(yParam).doubleValue());
            if (xi >= 0 && yi >= 0 && r.metrics() != null) {
                grid[yi][xi] = r.metrics().sortinoRatio();
            }
        }
        List<String> colLabels = xs.stream().map(Svg::fmt).toList();
        List<String> rowLabels = ys.stream().map(Svg::fmt).toList();
        return Svg.heatmap(colLabels, rowLabels, grid,
                "Sortino: " + xParam + " (x) × " + yParam + " (y)");
    }

    private static void phase2(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getSelections().isEmpty()) return;
        h.append("<h2>Phase 2 — Robust Parameter Selection</h2>");
        for (Map.Entry<Timeframe, SelectionResult> e : ctx.getSelections().entrySet()) {
            SelectionResult sel = e.getValue();
            h.append(String.format("<div class='card'><h3>%s → selected <span class='mono'>%s</span>"
                            + " (Sortino %.2f, %d trades)</h3>",
                    e.getKey().getCode(), esc(sel.combination().id()),
                    sel.selectedResult().metrics().sortinoRatio(),
                    sel.selectedResult().metrics().totalTrades()));
            h.append("<table><tr><th>Parameter</th><th class='num'>Selected</th>"
                    + "<th class='num'>Plateau low</th><th class='num'>Plateau high</th>"
                    + "<th class='num'>Robustness</th><th>Stable</th></tr>");
            for (ParameterSelection p : sel.perParameter().values()) {
                h.append(String.format(
                        "<tr><td class='mono'>%s</td><td class='num'>%s</td>"
                                + "<td class='num'>%s</td><td class='num'>%s</td>"
                                + "<td class='num'>%s</td><td class='%s'>%s</td></tr>",
                        esc(p.parameter()), fmt(p.selectedValue()),
                        fmtOrDash(p.regionLower()), fmtOrDash(p.regionUpper()),
                        fmtOrDash(p.robustnessScore()),
                        p.stable() ? "ok" : "bad", p.stable() ? "STABLE" : "UNSTABLE"));
            }
            h.append("</table>");
            for (PlateauResult pr : sel.plateaus().values()) {
                h.append(Svg.sensitivityChart(pr.curve(),
                        pr.region() != null ? pr.region().lowerBound() : null,
                        pr.region() != null ? pr.region().upperBound() : null,
                        pr.parameterName() + " → Sortino"));
                h.append(String.format("<div class='reason'>%s — %s</div>",
                        esc(pr.parameterName()), esc(pr.reason())));
            }
            if (!sel.warnings().isEmpty()) {
                h.append("<div>");
                sel.warnings().forEach(w ->
                        h.append("<span class='warn'>").append(w).append("</span>"));
                h.append("</div>");
            }
            h.append("</div>");
        }
    }

    private static void phase3(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getWalkForwardResults().isEmpty()) return;
        h.append("<h2>Phase 3 — Walk-Forward Validation</h2>");
        for (Map.Entry<Timeframe, WalkForwardResult> e : ctx.getWalkForwardResults().entrySet()) {
            WalkForwardResult wf = e.getValue();
            h.append(String.format("<div class='card'><h3>%s — WFE %.2f | "
                            + "mean IS Sortino %.2f | mean OOS Sortino %.2f | %d OOS trades</h3>",
                    e.getKey().getCode(), wf.walkForwardEfficiency(),
                    wf.meanInSampleSortino(), wf.meanOutOfSampleSortino(), wf.totalOosTrades()));
            List<double[]> pairs = new ArrayList<>();
            for (WalkForwardWindowResult w : wf.windows()) {
                pairs.add(new double[]{
                        w.inSample() != null ? w.inSample().sortinoRatio() : 0,
                        w.outOfSample() != null ? w.outOfSample().sortinoRatio() : 0});
            }
            h.append(Svg.walkForwardChart(pairs, "Sortino per window"));
            h.append("<table><tr><th>#</th><th>Train</th><th>Test</th><th>Combination</th>"
                    + "<th class='num'>IS Sortino</th><th class='num'>OOS Sortino</th>"
                    + "<th class='num'>OOS trades</th></tr>");
            for (WalkForwardWindowResult w : wf.windows()) {
                h.append(String.format(
                        "<tr><td class='num'>%d</td><td>%s → %s</td><td>%s → %s</td>"
                                + "<td class='mono'>%s</td><td class='num'>%s</td>"
                                + "<td class='num'>%s</td><td class='num'>%s</td></tr>",
                        w.windowIndex(),
                        TS_FMT.format(Instant.ofEpochMilli(w.trainStart())),
                        TS_FMT.format(Instant.ofEpochMilli(w.trainEnd())),
                        TS_FMT.format(Instant.ofEpochMilli(w.trainEnd())),
                        TS_FMT.format(Instant.ofEpochMilli(w.testEnd())),
                        w.failed() ? "FAILED: " + esc(w.errorMessage()) : esc(w.combination().id()),
                        w.inSample() != null ? String.format("%.2f", w.inSample().sortinoRatio()) : "-",
                        w.outOfSample() != null ? String.format("%.2f", w.outOfSample().sortinoRatio()) : "-",
                        w.outOfSample() != null ? w.outOfSample().totalTrades() : "-"));
            }
            h.append("</table>");
            if (!wf.parameterDrift().isEmpty()) {
                h.append("<h3>Parameter drift across windows</h3><table><tr><th>Parameter</th>"
                        + "<th class='num'>Mean</th><th class='num'>Median</th>"
                        + "<th class='num'>StdDev</th><th class='num'>Min</th>"
                        + "<th class='num'>Max</th><th class='num'>CV</th><th></th></tr>");
                for (ParameterDriftStats d : wf.parameterDrift().values()) {
                    h.append(String.format(
                            "<tr><td class='mono'>%s</td><td class='num'>%.3g</td>"
                                    + "<td class='num'>%.3g</td><td class='num'>%.3g</td>"
                                    + "<td class='num'>%.3g</td><td class='num'>%.3g</td>"
                                    + "<td class='num'>%.3f</td><td class='%s'>%s</td></tr>",
                            esc(d.parameter()), d.mean(), d.median(), d.stdDev(),
                            d.min(), d.max(), d.coefficientOfVariation(),
                            d.stable() ? "ok" : "bad", d.stable() ? "STABLE" : "UNSTABLE"));
                }
                h.append("</table>");
            }
            h.append("</div>");
        }
    }

    private static void phase4(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getExitResults().isEmpty()) return;
        h.append("<h2>Phase 4 — Exit Optimization (MAE/MFE)</h2>");
        for (Map.Entry<String, ExitOptimizationResult> e : ctx.getExitResults().entrySet()) {
            ExitOptimizationResult er = e.getValue();
            List<Excursion> ex = er.excursions();
            ExcursionAnalyzer.Percentiles mae =
                    ExcursionAnalyzer.percentiles(ex, Excursion::maeAtr);
            ExcursionAnalyzer.Percentiles mfe =
                    ExcursionAnalyzer.percentiles(ex, Excursion::mfeAtr);
            h.append(String.format("<div class='card'><h3>%s @ %s → policy "
                            + "<span class='mono'>stop=%sA target=%sA timeStop=%sb</span>"
                            + " — Sortino %.2f, %d trades, maxDD %.2f%%</h3>",
                    esc(er.entryCombination().id()), e.getKey().split("\\|")[0],
                    fmtOrDash(er.policy().initialStopAtr()),
                    fmtOrDash(er.policy().targetAtr()),
                    er.policy().timeStopBars(),
                    er.metrics().sortinoRatio(), er.metrics().totalTrades(),
                    er.metrics().maxDrawdown()));
            h.append(String.format(
                    "<div class='mono'>MAE_ATR  p25=%.2f p50=%.2f p75=%.2f p90=%.2f<br/>"
                            + "MFE_ATR  p25=%.2f p50=%.2f p75=%.2f p90=%.2f</div>",
                    mae.p25(), mae.p50(), mae.p75(), mae.p90(),
                    mfe.p25(), mfe.p50(), mfe.p75(), mfe.p90()));
            h.append("</div>");
        }
    }

    private static void phase5(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getFilterAnalyses().isEmpty()) return;
        h.append("<h2>Phase 5 — Filter &amp; Feature Analysis</h2>");
        for (FilterAnalysis fa : ctx.getFilterAnalyses()) {
            h.append(String.format("<div class='card'><h3>%s — %s</h3>"
                            + "<div class='reason'>%s</div>",
                    esc(fa.feature()),
                    fa.accepted()
                            ? String.format("<span class='ok'>ACCEPTED</span> region [%.4g, %.4g] "
                                    + "(improvement %+.3f%%)", fa.regionLower(),
                                    fa.regionUpper(), fa.improvement())
                            : "<span class='bad'>rejected</span>",
                    esc(fa.reason())));
            if (!fa.bins().isEmpty()) {
                h.append("<table><tr><th>Bin</th><th class='num'>Trades</th>"
                        + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                        + "<th class='num'>AvgR</th><th class='num'>Sortino</th>"
                        + "<th class='num'>PF</th></tr>");
                for (BucketStats b : fa.bins()) {
                    h.append(String.format(
                            "<tr><td class='mono'>[%.4g, %.4g)</td><td class='num'>%d</td>"
                                    + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                    + "<td class='num'>%s</td><td class='num'>%.2f</td>"
                                    + "<td class='num'>%s</td></tr>",
                            b.lowerBound(), b.upperBound(), b.tradeCount(), b.winRate(),
                            b.avgPnlPercent(), Double.isNaN(b.avgR()) ? "-"
                                    : String.format("%.2f", b.avgR()),
                            b.sortino(), finite((float) b.profitFactor())));
                }
                h.append("</table>");
                h.append(bucketStrip(fa.bins(), fa.feature() + " — avg P&L% per bin"));
            }
            h.append("</div>");
        }
    }

    /** Single-row heatmap of avg-P&L% across bins/buckets. */
    private static String bucketStrip(List<BucketStats> bins, String title) {
        if (bins.isEmpty()) return "";
        List<String> cols = new ArrayList<>();
        double[][] v = new double[1][bins.size()];
        for (int i = 0; i < bins.size(); i++) {
            BucketStats b = bins.get(i);
            cols.add(b.label() != null ? b.label()
                    : String.format("%.3g–%.3g", b.lowerBound(), b.upperBound()));
            v[0][i] = b.tradeCount() == 0 ? Double.NaN : b.avgPnlPercent();
        }
        return Svg.heatmap(cols, List.of("avgPnl%"), v, title);
    }

    private static void phase6(StringBuilder h, OptimizationContext ctx) {
        var sel = ctx.getTimeframeSelection();
        if (sel == null) return;
        h.append("<h2>Phase 6 — Timeframe Selection</h2><div class='card'>");
        h.append(String.format("<div><b>Selected: %s</b> — %s</div>",
                sel.selected() != null ? sel.selected().getCode() : "none",
                esc(sel.reason())));
        h.append("<table><tr><th>TF</th><th class='num'>Sortino</th>"
                + "<th class='num'>Trades</th><th class='num'>MaxDD%</th>"
                + "<th class='num'>Stable params%</th><th class='num'>WFE</th>"
                + "<th class='num'>Robustness</th></tr>");
        for (var a : sel.perTimeframe()) {
            h.append(String.format(
                    "<tr><td class='mono'>%s</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%d</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%.0f</td><td class='num'>%s</td>"
                            + "<td class='num'>%.3f</td></tr>",
                    a.timeframe().getCode(), a.sortino(), a.trades(), a.maxDrawdown(),
                    a.stableParameterShare() * 100,
                    Double.isNaN(a.walkForwardEfficiency()) ? "-"
                            : String.format("%.2f", a.walkForwardEfficiency()),
                    a.robustnessScore()));
        }
        h.append("</table></div>");
    }

    private static void phase7(StringBuilder h, OptimizationContext ctx) {
        TradeRun run = ctx.getFinalTradeRun();
        boolean hasAny = run != null || !ctx.getCalendarAnalyses().isEmpty();
        if (!hasAny) return;
        h.append("<h2>Phase 7 — Exploratory Trade Analysis</h2>");
        if (run != null) {
            h.append(String.format("<div class='kpi' style='display:inline-block'>"
                            + "<div class='v'>%d</div><div class='k'>trades analyzed</div></div>",
                    run.observations().size()));
        }
        for (Map.Entry<String, List<BucketStats>> e : ctx.getCalendarAnalyses().entrySet()) {
            h.append(String.format("<h3>%s</h3>", esc(e.getKey())));
            h.append("<table><tr><th>Bucket</th><th class='num'>Trades</th>"
                    + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                    + "<th class='num'>AvgR</th><th class='num'>Sortino</th>"
                    + "<th class='num'>PF</th></tr>");
            for (BucketStats b : e.getValue()) {
                h.append(String.format(
                        "<tr><td>%s</td><td class='num'>%d</td>"
                                + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                + "<td class='num'>%s</td><td class='num'>%.2f</td>"
                                + "<td class='num'>%s</td></tr>",
                        esc(b.label() != null ? b.label()
                                : String.format("[%.4g,%.4g)", b.lowerBound(), b.upperBound())),
                        b.tradeCount(), b.winRate(), b.avgPnlPercent(),
                        Double.isNaN(b.avgR()) ? "-" : String.format("%.2f", b.avgR()),
                        b.sortino(), finite((float) b.profitFactor())));
            }
            h.append("</table>");
            h.append(bucketStrip(e.getValue(), e.getKey() + " — avg P&L%"));
        }
        var st = ctx.getStreakResult();
        if (st != null) {
            h.append("<h3>Streaks &amp; transitions</h3><div class='card'>");
            var t = st.transitions();
            h.append(String.format("<div class='mono'>P(W|W)=%.2f&nbsp;&nbsp;P(L|W)=%.2f"
                            + "&nbsp;&nbsp;P(W|L)=%.2f&nbsp;&nbsp;P(L|L)=%.2f"
                            + "&nbsp;&nbsp;(unconditional win %.1f%%, n=%d)</div>",
                    t.pWinGivenWin(), t.pLossGivenWin(), t.pWinGivenLoss(), t.pLossGivenLoss(),
                    t.unconditionalWin() * 100, t.samples()));
            streakTable(h, "After win streak", st.afterWinStreaks());
            streakTable(h, "After loss streak", st.afterLossStreaks());
            h.append("</div>");
        }
        var acf = ctx.getAutocorrelationResult();
        if (acf != null) {
            h.append(String.format("<h3>Return autocorrelation (n=%d, 95%% bound ±%.3f)</h3>",
                    acf.samples(), acf.significanceBound()));
            h.append("<div class='mono'>lag→acf: ");
            for (int i = 1; i < acf.acf().length; i++) {
                String cls = Math.abs(acf.acf()[i]) > acf.significanceBound() ? "bad" : "";
                h.append(String.format("<span class='%s'>%d:%+.3f</span> ", cls, i, acf.acf()[i]));
            }
            h.append(String.format("<br/>Ljung-Box Q(%d)=%.1f — %s</div>",
                    acf.lags(), acf.ljungBoxQ(),
                    acf.significant() ? "<span class='bad'>significant serial structure</span>"
                            : "<span class='ok'>no significant serial structure</span>"));
        }
        var rm = ctx.getRMultipleResult();
        if (rm != null && rm.samples() > 0) {
            h.append(String.format("<h3>R-multiples (n=%d) — %s</h3><div class='mono'>"
                            + "mean=%.2f median=%.2f std=%.2f skew=%.2f kurt=%.2f<br/>"
                            + "p5=%.2f p25=%.2f p75=%.2f p95=%.2f</div>",
                    rm.samples(), RMultipleAnalyzer.classifySkew(rm.skewness()),
                    rm.mean(), rm.median(), rm.stdDev(), rm.skewness(), rm.kurtosis(),
                    rm.p5(), rm.p25(), rm.p75(), rm.p95()));
        }
        var dd = ctx.getDrawdownResult();
        if (dd != null && dd.drawdownEpisodes() > 0) {
            h.append(String.format("<h3>Drawdown</h3><div class='mono'>"
                            + "max=%.2f%% avg=%.2f%% median=%.2f%% p95=%.2f%% p99=%.2f%%"
                            + " | episodes=%d maxDur=%.1fd avgDur=%.1fd</div>",
                    dd.maxDrawdown(), dd.avgDrawdown(), dd.medianDrawdown(),
                    dd.p95Drawdown(), dd.p99Drawdown(), dd.drawdownEpisodes(),
                    dd.maxDrawdownDurationDays(), dd.avgDrawdownDurationDays()));
        }
        for (var rr : ctx.getRegimeResults()) {
            h.append(String.format("<h3>Regime: %s</h3>", esc(rr.seriesName())));
            if (rr.bins().isEmpty()) {
                h.append("<div class='reason'>insufficient data</div>");
            } else {
                h.append("<table><tr><th>Bin</th><th class='num'>Trades</th>"
                        + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                        + "<th class='num'>PF</th></tr>");
                for (BucketStats b : rr.bins()) {
                    h.append(String.format(
                            "<tr><td class='mono'>[%.3g, %.3g)</td><td class='num'>%d</td>"
                                    + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                    + "<td class='num'>%s</td></tr>",
                            b.lowerBound(), b.upperBound(), b.tradeCount(), b.winRate(),
                            b.avgPnlPercent(), finite((float) b.profitFactor())));
                }
                h.append("</table>");
                if (rr.stableRegion() != null) {
                    h.append(String.format("<div class='reason'>stable profitable region "
                                    + "[%.4g, %.4g]</div>",
                            rr.stableRegion()[0], rr.stableRegion()[1]));
                }
            }
        }
    }

    private static void streakTable(StringBuilder h, String title,
                                     List<StreakAnalyzer.StreakBucket> buckets) {
        h.append(String.format("<h3>%s</h3><table><tr><th>Streak</th>"
                + "<th class='num'>Samples</th><th class='num'>P(next win)%%</th>"
                + "<th class='num'>AvgR</th><th class='num'>MedianR</th></tr>", esc(title)));
        for (var b : buckets) {
            h.append(String.format("<tr><td class='num'>%d</td><td class='num'>%d</td>"
                            + "<td class='num'>%s</td><td class='num'>%s</td>"
                            + "<td class='num'>%s</td></tr>",
                    b.streakLength(), b.samples(),
                    Double.isNaN(b.probNextWin()) ? "-" : String.format("%.1f", b.probNextWin()),
                    Double.isNaN(b.avgNextR()) ? "-" : String.format("%.2f", b.avgNextR()),
                    Double.isNaN(b.medianNextR()) ? "-" : String.format("%.2f", b.medianNextR())));
        }
        h.append("</table>");
    }

    private static void phase8(StringBuilder h, OptimizationContext ctx) {
        var mc = ctx.getMonteCarloResult();
        var cs = ctx.getCostSensitivityResult();
        var fc = ctx.getFinalConfiguration();
        if (mc == null && cs == null && fc == null) return;
        h.append("<h2>Phase 8 — Statistical Validation</h2>");
        if (mc != null) {
            h.append(String.format("<div class='card'><h3>Monte Carlo "
                            + "(%d simulations, seed %d)</h3><div class='mono'>"
                            + "historical maxDD %.2f%% | median %.2f%% | p95 %.2f%% | p99 %.2f%%<br/>"
                            + "median net profit %.0f | p5 net %.0f | P(loss)=%.1f%%</div></div>",
                    mc.simulations(), mc.seed(), mc.historicalMaxDrawdown(),
                    mc.medianMaxDrawdown(), mc.p95MaxDrawdown(), mc.p99MaxDrawdown(),
                    mc.medianNetProfit(), mc.p5NetProfit(), mc.probabilityOfLoss()));
        }
        if (cs != null) {
            h.append(String.format("<div class='card'><h3>Cost sensitivity — %s</h3>"
                            + "<table><tr><th class='num'>×</th>"
                            + "<th class='num'>Slippage%%</th><th class='num'>Sortino</th>"
                            + "<th class='num'>CAGR%%</th><th class='num'>MaxDD%%</th>"
                            + "<th class='num'>Trades</th></tr>",
                    cs.fragile() ? "<span class='bad'>FRAGILE</span>"
                            : "<span class='ok'>robust to costs</span>"));
            for (var p : cs.points()) {
                h.append(String.format(
                        "<tr><td class='num'>%.1f</td><td class='num'>%.3f</td>"
                                + "<td class='num'>%.2f</td><td class='num'>%.2f</td>"
                                + "<td class='num'>%.2f</td><td class='num'>%d</td></tr>",
                        p.slippageMultiplier(), p.slippagePercent(),
                        p.metrics().sortinoRatio(), p.metrics().cagr(),
                        p.metrics().maxDrawdown(), p.metrics().totalTrades()));
            }
            h.append("</table></div>");
        }
        if (fc != null) {
            h.append("<div class='card'><h3>Final configuration (export)</h3>"
                    + "<pre class='mono'>").append(esc(fc.toJson())).append("</pre></div>");
        }
    }

    private static void warnings(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getWarnings().isEmpty()) return;
        h.append("<h2>Research Warnings</h2><div>");
        Map<ResearchWarning, Long> counts = new java.util.EnumMap<>(ResearchWarning.class);
        for (ResearchWarning w : ctx.getWarnings()) {
            counts.merge(w, 1L, Long::sum);
        }
        counts.forEach((w, c) -> h.append("<span class='warn'>")
                .append(w).append(c > 1 ? " ×" + c : "").append("</span>"));
        h.append("</div>");
    }

    private static void audit(StringBuilder h, OptimizationContext ctx) {
        ResearchConfiguration c = ctx.getConfiguration();
        h.append("<h2>Research Audit Trail</h2><div class='card'><table>");
        row(h, "Strategy", ctx.getStrategyId());
        row(h, "Job id", ctx.getJob().getId());
        row(h, "Created", ctx.getJob().getCreatedAt().toString());
        row(h, "Tasks completed / failed",
                ctx.getJob().getCompletedTasks() + " / " + ctx.getJob().getFailedTasks());
        row(h, "Combinations tested", String.valueOf(ctx.getOptimizationResults().size()));
        row(h, "Plateau threshold", String.valueOf(c.getPlateauThreshold()));
        row(h, "Min plateau points", String.valueOf(c.getMinPlateauPoints()));
        row(h, "Min trades / combination", String.valueOf(c.getMinTradesPerCombination()));
        row(h, "Drift CV threshold", String.valueOf(c.getParameterDriftCvThreshold()));
        row(h, "Initial capital", String.valueOf(c.getInitialCapital()));
        row(h, "Cost % / slippage %", c.getCostPercent() + " / " + c.getSlippagePercent());
        ctx.getAuditData().forEach((k, v) -> row(h, k, v));
        h.append("</table></div>");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static void kpi(StringBuilder h, String key, String val) {
        h.append(String.format("<div class='kpi'><div class='v'>%s</div>"
                + "<div class='k'>%s</div></div>", val, key));
    }

    private static void row(StringBuilder h, String k, String v) {
        h.append(String.format("<tr><td>%s</td><td class='mono'>%s</td></tr>",
                esc(k), esc(v)));
    }

    private static String fmtOrDash(Double v) {
        return v == null || Double.isNaN(v) ? "-" : fmt(v);
    }

    private static String finite(float v) {
        return Float.isFinite(v) ? String.format("%.2f", v) : "∞";
    }

}
