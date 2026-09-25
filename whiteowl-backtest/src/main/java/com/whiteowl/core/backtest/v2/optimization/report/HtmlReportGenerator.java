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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.backtest.v2.optimization.report.Svg.esc;
import static com.whiteowl.core.backtest.v2.optimization.report.Svg.fmt;

/**
 * Phase 9 — self-contained HTML report generated from the
 * {@link OptimizationContext} alone. Inline CSS + inline SVG charts + a few
 * lines of vanilla JS for tabs and table/graph toggles — no external assets.
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
        StringBuilder h = new StringBuilder(128_000);
        h.append("""
                <!DOCTYPE html><html><head><meta charset='utf-8'>
                <title>Optimization Report</title><style>
                body{font-family:'Segoe UI',Arial,sans-serif;background:#0f1420;color:#dbe2ef;
                     margin:0;padding:24px;line-height:1.45}
                h1{font-size:22px;color:#fff;border-bottom:2px solid #2e6bd6;padding-bottom:8px}
                h2{font-size:17px;color:#7fb0ff;margin:28px 0 10px}
                h3{font-size:14px;color:#9db8dd;margin:18px 0 6px}
                table{border-collapse:collapse;width:100%;font-size:12px;margin:8px 0;
                     border-radius:8px;overflow:hidden}
                th{background:#1c2740;color:#8fb5f0;text-align:left;padding:6px 8px;
                   border-bottom:1px solid #2e3c5c}
                td{padding:5px 8px;border-bottom:1px solid #1e2a45}
                tr:nth-child(even) td{background:#141c30}
                .num{text-align:right;font-variant-numeric:tabular-nums}
                .card{background:linear-gradient(165deg,#1d2a46 0%,#162032 45%,#111a2c 100%);
                      border:1px solid #2c3d60;border-radius:12px;
                      padding:14px 18px;margin:12px 0;position:relative;overflow:hidden;
                      box-shadow:0 1px 0 rgba(255,255,255,.07) inset,
                                 0 10px 22px rgba(0,0,0,.5),0 2px 6px rgba(0,0,0,.4)}
                .card::before{content:'';position:absolute;top:0;left:0;right:0;height:45%;
                      background:linear-gradient(180deg,rgba(255,255,255,.06),
                              rgba(255,255,255,0));pointer-events:none;border-radius:12px 12px 0 0}
                .grid{display:flex;flex-wrap:wrap;gap:12px}
                .kpi{background:linear-gradient(165deg,#21304e 0%,#162032 55%,#111a2c 100%);
                      border:1px solid #2c3d60;border-radius:12px;
                      padding:12px 18px;min-width:130px;position:relative;overflow:hidden;
                      box-shadow:0 1px 0 rgba(255,255,255,.08) inset,
                                 0 6px 16px rgba(0,0,0,.5),0 2px 4px rgba(0,0,0,.35)}
                .kpi .v{font-size:18px;color:#fff;font-weight:600;text-shadow:0 1px 2px rgba(0,0,0,.6)}
                .kpi .k{font-size:11px;color:#8fa3c8;text-transform:uppercase}
                .warn{background:linear-gradient(165deg,#4a3618,#3a2a12);
                      border:1px solid #8a6a2e;color:#ffcf7a;
                      padding:4px 12px;border-radius:14px;display:inline-block;margin:2px;
                      box-shadow:0 2px 5px rgba(0,0,0,.4),0 1px 0 rgba(255,255,255,.1) inset}
                .ok{color:#7bd88f}.bad{color:#ff7a7a}
                .chart{width:100%;max-width:700px;background:#0d1522;border-radius:10px;
                       margin:8px 0;border:1px solid #223152;
                       box-shadow:0 4px 10px rgba(0,0,0,.35),
                                  0 1px 0 rgba(255,255,255,.04) inset}
                .chart.wide{max-width:100%}
                .chart .axis{stroke:#3a4a6b;stroke-width:1}
                .chart .line,.chart .eqLine{fill:none;stroke:#5aa2ff;stroke-width:1.5}
                .chart .pt{fill:#8fc1ff}
                .chart .plateau{fill:#2e6bd6;opacity:.22}
                .chart .is{fill:#5aa2ff}.chart .oos{fill:#f0a35e}
                .chart .ddLine{fill:none;stroke:#ff7a7a;stroke-width:1.2}
                .chart .ddArea{fill:#ff7a7a;opacity:.15}
                .chart .barPos{fill:#3f8f5f}.chart .barNeg{fill:#a03d3d}
                .chart .title{fill:#dbe2ef;font-size:13px}
                .chart .lbl{fill:#8fa3c8;font-size:10px}
                .chart .oosT{fill:#f0a35e}
                .mono{font-family:Consolas,monospace;font-size:12px;color:#9fd0a8}
                .reason{color:#8fa3c8;font-size:11px;font-style:italic}
                /* horizontal tab groups */
                .tabbar{display:flex;gap:4px;border-bottom:1px solid #26344f;margin-top:16px}
                .tabbtn{background:linear-gradient(180deg,#1a2440,#141c30);
                        border:1px solid #26344f;border-bottom:none;
                        padding:8px 16px;color:#8fa3c8;cursor:pointer;font-size:13px;
                        border-radius:10px 10px 0 0;
                        box-shadow:0 1px 0 rgba(255,255,255,.05) inset}
                .tabbtn.on{color:#fff;background:linear-gradient(180deg,#27395e,#1e2c48);
                        border-top:2px solid #4d8dff;
                        box-shadow:0 -2px 8px rgba(46,107,214,.35),
                                   0 1px 0 rgba(255,255,255,.08) inset}
                .tabcontent{display:none;padding:14px 0}
                .tabcontent.on{display:block}
                /* vertical tab group (timeframes) */
                .tabgroup.v{display:flex;gap:0;align-items:stretch;margin-top:14px;
                     border:1px solid #26344f;border-radius:12px;overflow:hidden;
                     background:#111a2c;
                     box-shadow:0 8px 20px rgba(0,0,0,.45),0 1px 0 rgba(255,255,255,.05) inset}
                .vtabstrip{display:flex;flex-direction:column;gap:0;padding:0;
                     background:#0c1322;border-right:1px solid #26344f}
                .vtab{writing-mode:vertical-rl;transform:rotate(180deg);
                     padding:18px 10px;color:#7d90b6;cursor:pointer;font-size:13px;
                     background:linear-gradient(90deg,#141c30,#101827);
                     border-bottom:1px solid #1e2a45;
                     letter-spacing:.06em;text-transform:uppercase;font-weight:600}
                .vtab:hover{background:#1a2440}
                .vtab.on{background:linear-gradient(90deg,#2e6bd6,#1e2c48);color:#fff;
                     border-bottom:1px solid #2e6bd6;
                     box-shadow:6px 0 14px rgba(46,107,214,.4)}
                .tabcontents{flex:1;min-width:0;padding:4px 18px 14px}
                /* sliding switch for table/graph — overlaid top-right of card */
                .tv{position:relative}
                .sw{display:inline-flex;align-items:center;gap:8px;cursor:pointer;
                    font-size:11px;color:#8fa3c8;user-select:none;
                    position:absolute;top:6px;right:10px;z-index:6;
                    background:rgba(13,21,34,.88);padding:4px 10px;border-radius:14px;
                    border:1px solid #2a3a5c;box-shadow:0 2px 8px rgba(0,0,0,.5)}
                .sw input{display:none}
                .sw .tr{width:38px;height:20px;border-radius:10px;position:relative;
                    background:#2a3652;transition:.2s;
                    box-shadow:inset 0 2px 4px rgba(0,0,0,.45),
                               0 1px 0 rgba(255,255,255,.06)}
                .sw .tr:before{content:'';position:absolute;width:16px;height:16px;
                    border-radius:50%;top:2px;left:2px;transition:.2s;
                    background:linear-gradient(180deg,#c9d6ee,#8fa3c8);
                    box-shadow:0 2px 4px rgba(0,0,0,.5)}
                .sw input:checked+.tr{background:#2e6bd6}
                .sw input:checked+.tr:before{transform:translateX(18px);
                    background:linear-gradient(180deg,#fff,#d5e2ff)}
                .sw .swlbl.on{color:#fff}
                </style><script>
                function woTab(e,id){
                  var grp=e.parentElement.parentElement;
                  var body=grp.querySelector(':scope>.tabcontents')||grp;
                  var kids=body.children;
                  for(var i=0;i<kids.length;i++){
                    if(kids[i].classList.contains('tabcontent'))kids[i].classList.remove('on');}
                  document.getElementById(id).classList.add('on');
                  var bar=e.parentElement;
                  for(var j=0;j<bar.children.length;j++)bar.children[j].classList.remove('on');
                  e.classList.add('on');
                }
                function woToggle(id,on){
                  var box=document.getElementById(id);
                  box.querySelector(':scope>.tv-table').style.display=on?'block':'none';
                  box.querySelector(':scope>.tv-graph').style.display=on?'none':'block';
                  var lbls=box.querySelectorAll(':scope>.sw>.swlbl');
                  lbls[0].className='swlbl'+(on?'':' on');
                  lbls[1].className='swlbl'+(on?' on':'');
                }
                </script></head><body>
                """);
        h.append("<h1>").append(esc(ctx.displayName())).append(" — Optimization Report</h1>");
        executiveSummary(h, ctx);
        timeframeTabs(h, ctx);
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

    // ── Executive summary ────────────────────────────────────────────────

    private static void executiveSummary(StringBuilder h, OptimizationContext ctx) {
        h.append("<h2>Executive Summary</h2><div class='grid'>");
        kpi(h, "Strategy", esc(ctx.displayName()));
        kpi(h, "Instruments", esc(String.join(", ", ctx.getScripIds())));
        kpi(h, "Timeframes", ctx.getTimeframes().size() + "");
        kpi(h, "Combinations tested", String.valueOf(ctx.getOptimizationResults().size()));
        kpi(h, "Job", esc(ctx.getJob().getId().substring(0, 8)));
        kpi(h, "Created", TS_FMT.format(ctx.getJob().getCreatedAt()));
        h.append("</div>");
        TradeRun run = ctx.getFinalTradeRun();
        if (run != null && run.equityCurve() != null && run.equityCurve().getSize() > 1) {
            h.append("<div class='card'><h3>Final configuration — equity &amp; drawdown</h3>");
            h.append(Svg.equityDrawdownChart(run.equityCurve().getValues(),
                    "Equity (top) / drawdown % (bottom)"));
            h.append("</div>");
        }
    }

    // ── Per-timeframe tab pane (Phases 1–3) ──────────────────────────────

    private static int uid = 0;
    private static String nid() { return "t" + (++uid); }

    private static void timeframeTabs(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getOptimizationResults().isEmpty()) return;
        h.append("<h2>Per-Timeframe Analysis</h2><div class='tabgroup v'>"
                + "<div class='vtabstrip'>");
        List<String> ids = new ArrayList<>();
        for (Timeframe tf : ctx.getTimeframes()) {
            String id = nid();
            ids.add(id);
            h.append(String.format("<div class='vtab%s' "
                            + "onclick='woTab(this,\"%s\")'>%s</div>",
                    ids.size() == 1 ? " on" : "", id, tf.getCode()));
        }
        h.append("</div><div class='tabcontents'>");
        int i = 0;
        for (Timeframe tf : ctx.getTimeframes()) {
            h.append(String.format("<div class='tabcontent%s' id='%s'>",
                    i == 0 ? " on" : "", ids.get(i++)));
            timeframeBody(h, ctx, tf);
            h.append("</div>");
        }
        h.append("</div></div>");
    }

    private static void timeframeBody(StringBuilder h, OptimizationContext ctx,
                                       Timeframe tf) {
        List<OptimizationResult> rs = ctx.resultsFor(tf);
        if (rs.isEmpty()) {
            h.append("<div class='reason'>no results</div>");
            return;
        }
        String[] labels = {"Grid results (Phase 1)", "Parameter selection (Phase 2)",
                "Walk-forward (Phase 3)"};
        List<String> ids = new ArrayList<>();
        h.append("<div class='tabgroup'><div class='tabbar'>");
        for (String l : labels) {
            String id = nid();
            ids.add(id);
            h.append(String.format("<button class='tabbtn%s' "
                            + "onclick='woTab(this,\"%s\")'>%s</button>",
                    ids.size() == 1 ? " on" : "", id, l));
        }
        h.append("</div><div class='tabcontents'>");

        // V-tab 1: grid results — bar chart of Sortino per combo + table.
        h.append(String.format("<div class='tabcontent on' id='%s'>", ids.get(0)));
        gridResultsTab(h, rs);
        h.append("</div>");

        // V-tab 2: robust parameter selection.
        h.append(String.format("<div class='tabcontent' id='%s'>", ids.get(1)));
        SelectionResult sel = ctx.getSelections().get(tf);
        if (sel == null) {
            h.append("<div class='reason'>no selection produced for this timeframe</div>");
        } else {
            selectionTab(h, sel);
        }
        h.append("</div>");

        // V-tab 3: walk-forward.
        h.append(String.format("<div class='tabcontent' id='%s'>", ids.get(2)));
        WalkForwardResult wf = ctx.getWalkForwardResults().get(tf);
        if (wf == null) {
            h.append("<div class='reason'>walk-forward not run for this timeframe</div>");
        } else {
            walkForwardTab(h, wf);
        }
        h.append("</div>");
        h.append("</div></div>");
    }

    private static void gridResultsTab(StringBuilder h, List<OptimizationResult> rs) {
        long ok = rs.stream()
                .filter(r -> r.status() == OptimizationResult.Status.OK).count();
        long thin = rs.stream()
                .filter(r -> r.status() == OptimizationResult.Status.INSUFFICIENT_SAMPLE).count();
        h.append(String.format("<div class='reason'>%d combinations — %d ok, "
                + "%d thin sample, %d failed</div>", rs.size(), ok, thin, rs.size() - ok - thin));

        List<OptimizationResult> sorted = rs.stream()
                .filter(r -> r.metrics() != null)
                .sorted(Comparator.comparingDouble(
                        (OptimizationResult r) -> r.metrics().sortinoRatio()).reversed())
                .toList();
        List<String> labels = sorted.stream().map(r -> r.combination().id()).toList();
        double[] sortinos = sorted.stream()
                .mapToDouble(r -> r.metrics().sortinoRatio()).toArray();
        String chart = Svg.barChart(labels, sortinos,
                "Sortino per combination (sorted)");
        StringBuilder tbl = new StringBuilder();
        tbl.append("<table><tr><th>Combination</th><th class='num'>Sortino</th>"
                + "<th class='num'>Sharpe</th><th class='num'>CAGR%</th>"
                + "<th class='num'>MaxDD%</th><th class='num'>Trades</th>"
                + "<th class='num'>WinRate%</th><th class='num'>Expectancy%</th>"
                + "<th class='num'>Payoff</th><th>Status</th></tr>");
        sorted.stream().limit(TOP_ROWS).forEach(r -> {
            OptimizationMetrics m = r.metrics();
            tbl.append(String.format(
                    "<tr><td class='mono'>%s</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%.2f</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%.2f</td><td class='num'>%d</td>"
                            + "<td class='num'>%.1f</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%s</td><td>%s</td></tr>",
                    esc(r.combination().id()), m.sortinoRatio(), m.sharpeRatio(),
                    m.cagr(), m.maxDrawdown(), m.totalTrades(), m.winRate(),
                    m.expectancy(), finite(m.payoffRatio()), r.status()));
        });
        tbl.append("</table>");
        toggle(h, tbl.toString(), chart);
    }

    private static void selectionTab(StringBuilder h, SelectionResult sel) {
        h.append(String.format("<div class='card'><h3>selected <span class='mono'>%s</span>"
                        + " (Sortino %.2f, %d trades)</h3>",
                esc(sel.combination().id()),
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
            String chart = Svg.sensitivityChart(pr.curve(),
                    pr.region() != null ? pr.region().lowerBound() : null,
                    pr.region() != null ? pr.region().upperBound() : null,
                    pr.parameterName() + " → Sortino");
            StringBuilder tbl = new StringBuilder(
                    "<table><tr><th>Value</th><th class='num'>Sortino</th>"
                            + "<th class='num'>Trades</th></tr>");
            pr.curve().forEach(p -> tbl.append(String.format(
                    "<tr><td class='num'>%s</td><td class='num'>%.2f</td>"
                            + "<td class='num'>%d</td></tr>",
                    fmt(p.parameterValue()), p.metric(), p.tradeCount())));
            tbl.append("</table>");
            toggle(h, tbl.toString(), chart);
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

    private static void walkForwardTab(StringBuilder h, WalkForwardResult wf) {
        h.append(String.format("<div class='reason'>WFE %.2f | mean IS Sortino %.2f | "
                        + "mean OOS Sortino %.2f | %d OOS trades</div>",
                wf.walkForwardEfficiency(), wf.meanInSampleSortino(),
                wf.meanOutOfSampleSortino(), wf.totalOosTrades()));
        List<double[]> pairs = new ArrayList<>();
        List<String> wlabels = new ArrayList<>();
        for (WalkForwardWindowResult w : wf.windows()) {
            pairs.add(new double[]{
                    w.inSample() != null ? w.inSample().sortinoRatio() : 0,
                    w.outOfSample() != null ? w.outOfSample().sortinoRatio() : 0});
            wlabels.add("w" + w.windowIndex());
        }
        String chart = Svg.walkForwardChart(pairs, "Sortino per window");
        StringBuilder tbl = new StringBuilder(
                "<table><tr><th>#</th><th>Train</th><th>Test</th><th>Combination</th>"
                        + "<th class='num'>IS Sortino</th><th class='num'>OOS Sortino</th>"
                        + "<th class='num'>OOS trades</th></tr>");
        for (WalkForwardWindowResult w : wf.windows()) {
            tbl.append(String.format(
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
        tbl.append("</table>");
        toggle(h, tbl.toString(), chart);
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
    }

    // ── Phase 4: exits ───────────────────────────────────────────────────

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

    // ── Phase 5: filter & feature analysis — tab per feature ────────────

    private static void phase5(StringBuilder h, OptimizationContext ctx) {
        if (ctx.getFilterAnalyses().isEmpty()) return;
        h.append("<h2>Phase 5 — Filter &amp; Feature Analysis</h2>"
                + "<div class='tabgroup'><div class='tabbar'>");
        List<String> ids = new ArrayList<>();
        for (FilterAnalysis fa : ctx.getFilterAnalyses()) {
            String id = nid();
            ids.add(id);
            h.append(String.format("<button class='tabbtn%s' "
                            + "onclick='woTab(this,\"%s\")'>%s</button>",
                    ids.size() == 1 ? " on" : "", id, esc(fa.feature())));
        }
        h.append("</div><div class='tabcontents'>");
        int i = 0;
        for (FilterAnalysis fa : ctx.getFilterAnalyses()) {
            h.append(String.format("<div class='tabcontent%s' id='%s'>",
                    i == 0 ? " on" : "", ids.get(i++)));
            h.append(String.format("<div>%s</div><div class='reason'>%s</div>",
                    fa.accepted()
                            ? String.format("<span class='ok'>ACCEPTED</span> region "
                                    + "[%.4g, %.4g] (improvement %+.3f%%)",
                                    fa.regionLower(), fa.regionUpper(), fa.improvement())
                            : "<span class='bad'>rejected</span>",
                    esc(fa.reason())));
            if (!fa.bins().isEmpty()) {
                String[] xLabels = fa.bins().stream()
                        .map(b -> String.format("%.3g–%.3g", b.lowerBound(), b.upperBound()))
                        .toArray(String[]::new);
                Map<String, double[]> series = new LinkedHashMap<>();
                series.put("binSortino", fa.bins().stream()
                        .mapToDouble(BucketStats::sortino).toArray());
                series.put("winRate%", fa.bins().stream()
                        .mapToDouble(BucketStats::winRate).toArray());
                String chart = Svg.multiLineChart(xLabels, series,
                        fa.feature() + " — Sortino & winRate% if only bin trades kept");
                StringBuilder tbl = new StringBuilder(
                        "<table><tr><th>Bin</th><th class='num'>Trades</th>"
                                + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                                + "<th class='num'>AvgR</th><th class='num'>Sortino</th>"
                                + "<th class='num'>PF</th></tr>");
                for (BucketStats b : fa.bins()) {
                    tbl.append(String.format(
                            "<tr><td class='mono'>[%.4g, %.4g)</td><td class='num'>%d</td>"
                                    + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                    + "<td class='num'>%s</td><td class='num'>%.2f</td>"
                                    + "<td class='num'>%s</td></tr>",
                            b.lowerBound(), b.upperBound(), b.tradeCount(), b.winRate(),
                            b.avgPnlPercent(), Double.isNaN(b.avgR()) ? "-"
                                    : String.format("%.2f", b.avgR()),
                            b.sortino(), finite((float) b.profitFactor())));
                }
                tbl.append("</table>");
                toggle(h, tbl.toString(), chart);
            }
            h.append("</div>");
        }
        h.append("</div></div>");
    }

    // ── Phase 6: timeframe selection ─────────────────────────────────────

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

    // ── Phase 7: exploratory analysis — bar charts + table toggles ──────

    private static void phase7(StringBuilder h, OptimizationContext ctx) {
        TradeRun run = ctx.getFinalTradeRun();
        if (run == null && ctx.getCalendarAnalyses().isEmpty()) return;
        h.append("<h2>Phase 7 — Exploratory Trade Analysis</h2>");
        if (run != null) {
            h.append(String.format("<div class='kpi' style='display:inline-block'>"
                            + "<div class='v'>%d</div><div class='k'>trades analyzed</div></div>",
                    run.observations().size()));
        }
        if (!ctx.getCalendarAnalyses().isEmpty()) {
            h.append("<div class='tabgroup'><div class='tabbar'>");
            List<String> calIds = new ArrayList<>();
            for (String name : ctx.getCalendarAnalyses().keySet()) {
                String id = nid();
                calIds.add(id);
                h.append(String.format("<button class='tabbtn%s' "
                                + "onclick='woTab(this,\"%s\")'>%s</button>",
                        calIds.size() == 1 ? " on" : "", id, esc(name)));
            }
            h.append("</div><div class='tabcontents'>");
            int ci = 0;
            for (Map.Entry<String, List<BucketStats>> e
                    : ctx.getCalendarAnalyses().entrySet()) {
                h.append(String.format("<div class='tabcontent%s' id='%s'>",
                        ci == 0 ? " on" : "", calIds.get(ci++)));
                List<BucketStats> buckets = e.getValue();
                List<String> labels = new ArrayList<>();
                double[] vals = new double[buckets.size()];
                int bi = 0;
                for (BucketStats b : buckets) {
                    labels.add(b.label() != null ? b.label()
                            : String.format("[%.3g,%.3g)", b.lowerBound(), b.upperBound()));
                    vals[bi++] = b.avgPnlPercent();
                }
                String chart = Svg.barChart(labels, vals, e.getKey() + " — avg P&L%");
                StringBuilder tbl = new StringBuilder(
                        "<table><tr><th>Bucket</th><th class='num'>Trades</th>"
                                + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                                + "<th class='num'>AvgR</th><th class='num'>Sortino</th>"
                                + "<th class='num'>PF</th></tr>");
                for (BucketStats b : buckets) {
                    tbl.append(String.format(
                            "<tr><td>%s</td><td class='num'>%d</td>"
                                    + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                    + "<td class='num'>%s</td><td class='num'>%.2f</td>"
                                    + "<td class='num'>%s</td></tr>",
                            esc(b.label() != null ? b.label()
                                    : String.format("[%.4g,%.4g)", b.lowerBound(),
                                    b.upperBound())),
                            b.tradeCount(), b.winRate(), b.avgPnlPercent(),
                            Double.isNaN(b.avgR()) ? "-" : String.format("%.2f", b.avgR()),
                            b.sortino(), finite((float) b.profitFactor())));
                }
                tbl.append("</table>");
                toggle(h, tbl.toString(), chart);
                h.append("</div>");
            }
            h.append("</div></div>");
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
            streakBlock(h, "After win streak — P(next win) %", st.afterWinStreaks());
            streakBlock(h, "After loss streak — P(next win) %", st.afterLossStreaks());
            h.append("</div>");
        }
        var acf = ctx.getAutocorrelationResult();
        if (acf != null && acf.acf().length > 1) {
            List<String> lagLabels = new ArrayList<>();
            double[] acfVals = new double[acf.acf().length - 1];
            for (int i = 1; i < acf.acf().length; i++) {
                lagLabels.add(String.valueOf(i));
                acfVals[i - 1] = acf.acf()[i];
            }
            h.append(String.format("<div class='card'><h3>Return autocorrelation "
                            + "(n=%d, 95%% bound ±%.3f)</h3>",
                    acf.samples(), acf.significanceBound()));
            h.append(Svg.barChart(lagLabels, acfVals, "ACF per lag"));
            h.append(String.format("<div class='mono'>Ljung-Box Q(%d)=%.1f — %s</div></div>",
                    acf.lags(), acf.ljungBoxQ(),
                    acf.significant() ? "<span class='bad'>significant serial structure</span>"
                            : "<span class='ok'>no significant serial structure</span>"));
        }
        var rm = ctx.getRMultipleResult();
        if (rm != null && rm.samples() > 0) {
            List<String> rLabels = new ArrayList<>();
            double[] rCounts = new double[rm.histogramBins().size()];
            int ri = 0;
            for (BucketStats b : rm.histogramBins()) {
                rLabels.add(String.format("%.2g", b.lowerBound()));
                rCounts[ri++] = b.tradeCount();
            }
            h.append(String.format("<div class='card'><h3>R-multiples (n=%d) — %s</h3>",
                    rm.samples(), RMultipleAnalyzer.classifySkew(rm.skewness())));
            h.append(Svg.barChart(rLabels, rCounts, "R-multiple distribution"));
            h.append(String.format("<div class='mono'>mean=%.2f median=%.2f std=%.2f "
                            + "skew=%.2f kurt=%.2f | p5=%.2f p25=%.2f p75=%.2f p95=%.2f</div></div>",
                    rm.mean(), rm.median(), rm.stdDev(), rm.skewness(), rm.kurtosis(),
                    rm.p5(), rm.p25(), rm.p75(), rm.p95()));
        }
        var dd = ctx.getDrawdownResult();
        if (dd != null && dd.drawdownEpisodes() > 0) {
            h.append(String.format("<div class='card'><h3>Drawdown</h3><div class='mono'>"
                            + "max=%.2f%% avg=%.2f%% median=%.2f%% p95=%.2f%% p99=%.2f%%"
                            + " | episodes=%d maxDur=%.1fd avgDur=%.1fd</div></div>",
                    dd.maxDrawdown(), dd.avgDrawdown(), dd.medianDrawdown(),
                    dd.p95Drawdown(), dd.p99Drawdown(), dd.drawdownEpisodes(),
                    dd.maxDrawdownDurationDays(), dd.avgDrawdownDurationDays()));
        }
        for (var rr : ctx.getRegimeResults()) {
            h.append(String.format("<h3>Regime: %s</h3>", esc(rr.seriesName())));
            if (rr.bins().isEmpty()) {
                h.append("<div class='reason'>insufficient data</div>");
            } else {
                List<String> rlabels = new ArrayList<>();
                double[] rvals = new double[rr.bins().size()];
                int bi = 0;
                for (BucketStats b : rr.bins()) {
                    rlabels.add(String.format("%.3g–%.3g", b.lowerBound(), b.upperBound()));
                    rvals[bi++] = b.avgPnlPercent();
                }
                String chart = Svg.barChart(rlabels, rvals,
                        rr.seriesName() + " — avg P&L%");
                StringBuilder tbl = new StringBuilder(
                        "<table><tr><th>Bin</th><th class='num'>Trades</th>"
                                + "<th class='num'>WinRate%</th><th class='num'>AvgPnl%</th>"
                                + "<th class='num'>PF</th></tr>");
                for (BucketStats b : rr.bins()) {
                    tbl.append(String.format(
                            "<tr><td class='mono'>[%.3g, %.3g)</td><td class='num'>%d</td>"
                                    + "<td class='num'>%.1f</td><td class='num'>%+.3f</td>"
                                    + "<td class='num'>%s</td></tr>",
                            b.lowerBound(), b.upperBound(), b.tradeCount(), b.winRate(),
                            b.avgPnlPercent(), finite((float) b.profitFactor())));
                }
                tbl.append("</table>");
                toggle(h, tbl.toString(), chart);
                if (rr.stableRegion() != null) {
                    h.append(String.format("<div class='reason'>stable profitable region "
                                    + "[%.4g, %.4g]</div>",
                            rr.stableRegion()[0], rr.stableRegion()[1]));
                }
            }
        }
    }

    private static void streakBlock(StringBuilder h, String title,
                                     List<StreakAnalyzer.StreakBucket> buckets) {
        List<String> labels = new ArrayList<>();
        double[] vals = new double[buckets.size()];
        int i = 0;
        for (var b : buckets) {
            labels.add("k=" + b.streakLength());
            vals[i++] = b.probNextWin();
        }
        String chart = Svg.barChart(labels, vals, title);
        StringBuilder tbl = new StringBuilder(String.format("<table><tr><th>Streak</th>"
                + "<th class='num'>Samples</th><th class='num'>P(next win)%%</th>"
                + "<th class='num'>AvgR</th><th class='num'>MedianR</th></tr>"));
        for (var b : buckets) {
            tbl.append(String.format("<tr><td class='num'>%d</td><td class='num'>%d</td>"
                            + "<td class='num'>%s</td><td class='num'>%s</td>"
                            + "<td class='num'>%s</td></tr>",
                    b.streakLength(), b.samples(),
                    Double.isNaN(b.probNextWin()) ? "-" : String.format("%.1f", b.probNextWin()),
                    Double.isNaN(b.avgNextR()) ? "-" : String.format("%.2f", b.avgNextR()),
                    Double.isNaN(b.medianNextR()) ? "-" : String.format("%.2f", b.medianNextR())));
        }
        tbl.append("</table>");
        toggle(h, tbl.toString(), chart);
    }

    // ── Phase 8: validation ──────────────────────────────────────────────

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

    // ── Warnings & audit ─────────────────────────────────────────────────

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
        row(h, "Strategy", ctx.displayName());
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

    /** Table/graph sliding switch — graph is the default view. */
    private static void toggle(StringBuilder h, String tableHtml, String chartHtml) {
        String id = nid();
        h.append(String.format("<div class='tv' id='%s'>"
                        + "<label class='sw'><span class='swlbl on'>Graph</span>"
                        + "<input type='checkbox' onchange='woToggle(\"%s\",this.checked)'>"
                        + "<span class='tr'></span><span class='swlbl'>Table</span></label>"
                        + "<div class='tv-graph'>%s</div>"
                        + "<div class='tv-table' style='display:none'>%s</div></div>",
                id, id, chartHtml, tableHtml));
    }

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
