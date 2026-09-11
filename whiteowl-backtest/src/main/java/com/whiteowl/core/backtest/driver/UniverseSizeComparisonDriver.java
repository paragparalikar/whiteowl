package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.universe.OrbCharacteristicFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Realistic slippage + liquidity sensitivity analysis for 1 Cr capital deployment.
 *
 * <h3>What this tests</h3>
 * <ul>
 *   <li>Slippage raised to 0.2% (0.002) — reflects real market impact at 1 Cr scale.</li>
 *   <li>Three ADV floors: 800L (8 Cr), 1000L (10 Cr), 1500L (15 Cr).</li>
 *   <li>Two strategy variants per floor:
 *       <br>(A) Full-17 = 6 Long-Aligned + 2 Short-Aligned + 9 Short-Opposite (17 slots, ~5.9L/stock)
 *       <br>(B) Lean-8  = 6 Long-Aligned + 2 Short-Aligned only            (8 slots,  ~12.5L/stock)
 *   </li>
 *   <li>Baseline: original 405-stock ORB Universe at 0.1% slippage (reference).</li>
 *   <li>Baseline-0.2%: same 405-stock universe at 0.2% slippage (honest comparison).</li>
 * </ul>
 */
public final class UniverseSizeComparisonDriver {

    private static final Logger log = LoggerFactory.getLogger(UniverseSizeComparisonDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ── Capital assumptions for 1 Cr ──────────────────────────────────
    private static final double CAPITAL           = 10_000_000; // 1 Cr in rupees
    private static final double SLIPPAGE_REF      = 0.001;      // 0.1% — reference/old value
    private static final double SLIPPAGE_REAL     = 0.002;      // 0.2% — realistic for 1 Cr

    // ADV floors to test (lakhs); 100L = 1 Cr
    private static final double[] ADV_FLOORS_LAKHS = { 800, 1000, 1500 };

    private static final String SOURCE_GROUP   = "ORB Universe - Top 1000 Turnover";
    private static final String BASELINE_GROUP = "ORB Universe";

    // ── Sub-strategy config factories ─────────────────────────────────

    /** Long Aligned — gap up, buy breakout, trail ATR */
    private static RotationalBacktestConfig longAligned(String group, double slippage) {
        return RotationalBacktestConfig.builder()
                .universeGroupName(group).openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30)).exitTime(LocalTime.of(15, 15))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.65)
                .trailingStopEnabled(true).trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.25).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.25)
                .slippage(slippage).initialCapital(CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.LONG)
                .minGapAtr(0.30).maxGapAtr(1.30).minOrbRvol(0.50).maxOrbIbs(0.90)
                .picks(6).build();
    }

    /** Short Aligned — gap down, short breakout, early exit */
    private static RotationalBacktestConfig shortAligned(String group, double slippage) {
        return RotationalBacktestConfig.builder()
                .universeGroupName(group).openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(11, 0)).exitTime(null)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(2.00)
                .trailingStopEnabled(true).trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.00).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(3.50)
                .slippage(slippage).initialCapital(CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(-1.55).maxGapAtr(-0.45).minOrbRvol(0.50).maxOrbIbs(0.60)
                .picks(2).build();
    }

    /** Short Opposite — gap up but short breakout (fade), 9 picks */
    private static RotationalBacktestConfig shortOpposite(String group, double slippage) {
        return RotationalBacktestConfig.builder()
                .universeGroupName(group).openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30)).exitTime(LocalTime.of(15, 20))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.80)
                .trailingStopEnabled(false).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.75)
                .slippage(slippage).initialCapital(CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(0.48).maxGapAtr(1.40).minOrbRvol(0.50).maxOrbIbs(0.90)
                .picks(9).build();
    }

    // ── Result record ─────────────────────────────────────────────────

    record BacktestRun(
            String label, int universeSize, double slippage, double advFloor,
            String variant,   // "Full-17" or "Lean-8"
            RotationalMetrics metrics, double netPnl, double avgPnl
    ) {}

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Slippage & Liquidity Sensitivity — 1 Cr Capital, 0.2% Slippage");
        log.info("  ADV floors: {}L, {}L, {}L", (long)ADV_FLOORS_LAKHS[0],
                (long)ADV_FLOORS_LAKHS[1], (long)ADV_FLOORS_LAKHS[2]);
        log.info("  Variants: Full-17 (6L+2SA+9SO) | Lean-8 (6L+2SA)");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        BarsRepository barsRepo = new FileBarsRepository();
        List<BacktestRun> results = new ArrayList<>();

        // ── Baselines ────────────────────────────────────────────────

        // Reference baseline (0.1% slippage, original universe) — for historical comparison
        log.info("=== BASELINE: {} @ 0.1% slippage (reference) ===", BASELINE_GROUP);
        results.add(runBacktest(barsRepo, BASELINE_GROUP,
                "Baseline-405 [0.1% slip, ref]", 405, SLIPPAGE_REF, 0, "Full-17"));

        // Honest baseline: same universe at 0.2% slippage
        log.info("=== BASELINE: {} @ 0.2% slippage ===", BASELINE_GROUP);
        results.add(runBacktest(barsRepo, BASELINE_GROUP,
                "Baseline-405 [0.2% slip]", 405, SLIPPAGE_REAL, 0, "Full-17"));

        // ── Load source pool once ─────────────────────────────────────
        log.info("Loading source pool '{}'...", SOURCE_GROUP);
        List<String> sourceIds = RotationalOrbDriver.loadGroupScrips(SOURCE_GROUP);
        Map<String, Bars> allDaily = RotationalOrbDriver.loadIntradayBars(barsRepo, sourceIds, Timeframe.DAILY);
        log.info("  Source pool: {} scrips, {} daily bars loaded", sourceIds.size(), allDaily.size());
        log.info("");

        // ── Three ADV floors × two variants ──────────────────────────
        for (double advFloor : ADV_FLOORS_LAKHS) {
            log.info("████████████  ADV floor: {}L ({}Cr)  ████████████",
                    (long) advFloor, (long)(advFloor / 100));

            // Score and filter
            var cfg = OrbCharacteristicFilter.FilterConfig.builder()
                    .minTurnoverLakhs(advFloor).minBars(100).build();
            List<OrbCharacteristicFilter.ScoredSymbol> ranked =
                    new OrbCharacteristicFilter(cfg).scoreAndRank(allDaily);
            int poolSize = ranked.size();
            log.info("  {} stocks pass >= {}L turnover floor", poolSize, (long) advFloor);

            // Print top 10 for this floor
            log.info("  Top 10 by composite score at {}L floor:", (long) advFloor);
            for (int i = 0; i < Math.min(10, ranked.size()); i++) {
                var s = ranked.get(i);
                log.info("    {:2d}. {:<20} score={:.4f}  spread={:.2f}%  ATR={:.2f}%  TO={}L",
                        i+1, s.symbol(), s.compositeScore(),
                        s.avgSpreadPct(), s.avgAtrPct(), (long) s.avgTurnoverLakhs());
            }

            // Save group file for this floor
            Path groupsDir = Path.of(System.getProperty("whiteowl.home",
                    System.getProperty("user.home") + java.io.File.separator + ".whiteowl"),
                    "data", "groups");
            String groupName = "ORB Liquid " + (long)(advFloor/100) + "Cr-" + poolSize;
            saveGroup(groupsDir, groupName, ranked.stream()
                    .map(OrbCharacteristicFilter.ScoredSymbol::symbol).toList());

            // Variant A: Full-17 (6L + 2SA + 9SO)
            results.add(runBacktest(barsRepo, groupName,
                    advFloor + "L (" + (long)(advFloor/100) + "Cr+) Full-17",
                    poolSize, SLIPPAGE_REAL, advFloor, "Full-17"));

            // Variant B: Lean-8 (6L + 2SA only — no short-opposite)
            results.add(runBacktest(barsRepo, groupName,
                    advFloor + "L (" + (long)(advFloor/100) + "Cr+) Lean-8",
                    poolSize, SLIPPAGE_REAL, advFloor, "Lean-8"));

            log.info("");
        }

        // ── Comparison table ──────────────────────────────────────────
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("  RESULTS SUMMARY");
        log.info("████████████████████████████████████████████████████████████████████████████████████████");
        log.info("");
        log.info("{}", String.format("%-42s %5s %5s %7s %8s %8s %6s %9s %8s %8s %10s",
                "Universe", "Size", "Slip", "Trades", "CAGR%", "Sortino", "Win%",
                "AvgPnL", "PF", "MaxDD%", "NetPnL"));
        log.info("{}", "─".repeat(145));
        for (BacktestRun r : results) {
            var m = r.metrics();
            log.info("{}", String.format("%-42s %5d %4.1f%% %7d %7.1f%% %8.2f %5.1f%% %9.0f %8.2f %7.1f%% %10s",
                    r.label(), r.universeSize(), r.slippage() * 100,
                    m.getTotalTrades(), m.getCagr() * 100, m.getSortino(),
                    m.getWinRate() * 100, r.avgPnl(), m.getProfitFactor(),
                    Math.abs(m.getMaxDrawdown()) * 100, fmtPnl(r.netPnl())));
        }

        // ── HTML report ───────────────────────────────────────────────
        log.info("");
        log.info("Generating HTML report...");
        Path reportPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports",
                "slippage_liquidity_sensitivity.html");
        reportPath.getParent().toFile().mkdirs();
        generateHtmlReport(reportPath, results);
        log.info("Report: {}", reportPath.toAbsolutePath());
        log.info("DONE");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Backtest runner
    // ══════════════════════════════════════════════════════════════════

    private static BacktestRun runBacktest(BarsRepository barsRepo, String groupName,
            String label, int knownSize, double slippage, double advFloor, String variant)
            throws Exception {
        log.info("─── {} ───", label);
        List<String> ids = RotationalOrbDriver.loadGroupScrips(groupName);
        Map<String, Bars> intra = RotationalOrbDriver.loadIntradayBars(barsRepo, ids, Timeframe.FIVE_MINUTE);
        Map<String, Bars> daily = RotationalOrbDriver.loadIntradayBars(barsRepo, ids, Timeframe.DAILY);

        List<CompositeOrbStrategy.SubStrategy> subs;
        int totalPicks;
        if ("Lean-8".equals(variant)) {
            subs = List.of(
                    new CompositeOrbStrategy.SubStrategy("Long-Aligned",  longAligned(groupName, slippage)),
                    new CompositeOrbStrategy.SubStrategy("Short-Aligned", shortAligned(groupName, slippage)));
            totalPicks = 8; // 6 + 2
        } else {
            subs = List.of(
                    new CompositeOrbStrategy.SubStrategy("Long-Aligned",  longAligned(groupName, slippage)),
                    new CompositeOrbStrategy.SubStrategy("Short-Aligned", shortAligned(groupName, slippage)),
                    new CompositeOrbStrategy.SubStrategy("Short-Opposite",shortOpposite(groupName, slippage)));
            totalPicks = 17; // 6 + 2 + 9
        }

        var strategy = new CompositeOrbStrategy(subs, totalPicks, slippage, CAPITAL);
        var result   = new CompositeBacktestEngine(strategy)
                .run(intra, daily, new ConfigurableRanker(true, true, false));

        var m      = result.getMetrics();
        var trades = result.getTradeLog();
        double net = trades.stream().mapToDouble(RotationalTrade::netPnl).sum();
        double avg = trades.isEmpty() ? 0 : net / trades.size();

        log.info("  CAGR={}%  Sortino={}  MaxDD={}%  Trades={}  WinRate={}%",
                String.format("%.1f", m.getCagr() * 100),
                String.format("%.2f", m.getSortino()),
                String.format("%.1f", Math.abs(m.getMaxDrawdown()) * 100),
                m.getTotalTrades(),
                String.format("%.1f", m.getWinRate() * 100));
        log.info("");
        return new BacktestRun(label, ids.size(), slippage, advFloor, variant, m, net, avg);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HTML Report
    // ══════════════════════════════════════════════════════════════════

    private static void generateHtmlReport(Path out, List<BacktestRun> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
            w.write("<title>Slippage & Liquidity Sensitivity — 1 Cr Capital</title>");
            w.write("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>");
            w.write("<style>" + CSS + "</style></head><body>");

            w.write("<div class=\"header\"><h1>Slippage &amp; Liquidity Sensitivity</h1>");
            w.write("<p class=\"subtitle\">Capital: ₹1 Cr &nbsp;|&nbsp; Slippage: 0.2% &nbsp;|&nbsp; ");
            w.write("ADV floors: 8 Cr / 10 Cr / 15 Cr &nbsp;|&nbsp; ");
            w.write("Variants: Full-17 slots vs Lean-8 slots</p></div>");

            // Assumption box
            w.write("<div class=\"callout\">");
            w.write("<strong>Capital allocation assumptions:</strong><br>");
            w.write("Full-17: ₹1Cr / 17 slots = <strong>₹5.9L per position</strong>. ");
            w.write("To keep impact ≤0.2%, order must be ≤1% of ADV → min ADV needed ≈ ₹590L = 6 Cr.<br>");
            w.write("Lean-8: ₹1Cr / 8 slots = <strong>₹12.5L per position</strong>. ");
            w.write("Min ADV needed ≈ ₹1250L = 12.5 Cr (at 1% of ADV rule).");
            w.write("</div>");

            // Main table
            w.write("<div class=\"section\"><h2>Full Comparison Table</h2>");
            w.write("<table class=\"stats-table\"><tr>");
            w.write("<th>Universe</th><th>Size</th><th>Slip</th><th>Variant</th><th>Trades</th>");
            w.write("<th>CAGR%</th><th>Sortino</th><th>Sharpe</th><th>Win%</th>");
            w.write("<th>Avg PnL</th><th>PF</th><th>Max DD%</th><th>Net PnL</th></tr>");
            double bestCagr = results.stream().mapToDouble(r -> r.metrics().getCagr()).max().orElse(0);
            for (BacktestRun r : results) {
                var m = r.metrics();
                boolean best = Math.abs(m.getCagr() - bestCagr) < 0.001;
                boolean isRef = r.label().contains("ref");
                w.write(String.format("<tr%s>",
                        best ? " class=\"best-row\"" : isRef ? " class=\"ref-row\"" : ""));
                w.write(String.format("<td>%s</td><td>%d</td><td>%.1f%%</td><td>%s</td><td>%d</td>",
                        r.label(), r.universeSize(), r.slippage() * 100, r.variant(), m.getTotalTrades()));
                w.write(String.format("<td>%.1f%%</td><td>%.2f</td><td>%.2f</td><td>%.1f%%</td>",
                        m.getCagr() * 100, m.getSortino(), m.getSharpe(), m.getWinRate() * 100));
                w.write(String.format("<td>%.0f</td><td>%.2f</td><td>%.1f%%</td><td>%s</td></tr>",
                        r.avgPnl(), m.getProfitFactor(), Math.abs(m.getMaxDrawdown()) * 100,
                        fmtPnl(r.netPnl())));
            }
            w.write("</table></div>");

            // CAGR chart
            w.write("<div class=\"section\"><h2>CAGR % by Configuration</h2>");
            w.write("<canvas id=\"cagrChart\" height=\"60\"></canvas></div>");

            // Sortino chart
            w.write("<div class=\"section\"><h2>Sortino Ratio by Configuration</h2>");
            w.write("<canvas id=\"sortinoChart\" height=\"60\"></canvas></div>");

            // Insight boxes
            w.write("<div class=\"section\"><h2>Key Insights for 1 Cr Deployment</h2>");
            w.write("<div class=\"insight-grid\">");
            w.write("<div class=\"insight\"><h3>Slippage cost</h3><p>Going from 0.1% to 0.2% slippage ");
            w.write("on the same 405-stock universe shows the pure cost of scaling. ");
            w.write("The delta vs the 0.1% baseline is entirely attributable to impact cost.</p></div>");
            w.write("<div class=\"insight\"><h3>ADV floor vs performance</h3><p>As the ADV floor rises, ");
            w.write("fewer high-spread/high-ATR stocks qualify, reducing the strategy's edge. ");
            w.write("The optimal floor balances liquidity safety with pool quality.</p></div>");
            w.write("<div class=\"insight\"><h3>Full-17 vs Lean-8</h3><p>Lean-8 removes Short-Opposite ");
            w.write("(9-pick sub-strategy). Each remaining position is ~2× larger (₹12.5L), ");
            w.write("requiring ~2× higher ADV floor to maintain the same impact assumption. ");
            w.write("Compare CAGR and Sortino carefully before choosing.</p></div>");
            w.write("<div class=\"insight\"><h3>Recommended floor</h3><p>For Full-17 at ₹1Cr capital: ");
            w.write("<strong>≥1000L (10 Cr)</strong> is a pragmatic balance — position is 0.59% of ADV. ");
            w.write("For Lean-8: use <strong>≥1500L (15 Cr)</strong> to keep impact cost manageable.</p></div>");
            w.write("</div></div>");

            // Chart JS
            w.write("<script>");
            var nonRef = results.stream().filter(r -> !r.label().contains("ref")).toList();
            String labels = "[" + nonRef.stream()
                    .map(r -> "'" + r.label().replace("'", "\\'") + "'")
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";
            String cagrData = "[" + nonRef.stream()
                    .map(r -> String.format("%.2f", r.metrics().getCagr() * 100))
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";
            String sortData = "[" + nonRef.stream()
                    .map(r -> String.format("%.2f", r.metrics().getSortino()))
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";
            String colors = "[" + nonRef.stream()
                    .map(r -> "Lean-8".equals(r.variant())
                            ? "'rgba(251,146,60,0.75)'" : "'rgba(96,165,250,0.75)'")
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + "]";

            writeBarChart(w, "cagrChart",    "CAGR %",       labels, cagrData, colors);
            writeBarChart(w, "sortinoChart", "Sortino Ratio", labels, sortData, colors);
            w.write("</script>");

            w.write("<div class=\"footer\">Generated " +
                    LocalDateTime.now(IST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " IST &nbsp;|&nbsp; Capital: ₹1 Cr &nbsp;|&nbsp; Blue = Full-17, Orange = Lean-8</div>");
            w.write("</body></html>");
        }
    }

    private static void writeBarChart(BufferedWriter w, String id, String title,
            String labels, String data, String colors) throws IOException {
        w.write("new Chart(document.getElementById('" + id + "'),{type:'bar',data:{labels:" + labels);
        w.write(",datasets:[{label:'" + title + "',data:" + data + ",backgroundColor:" + colors + "}]},");
        w.write("options:{responsive:true,plugins:{legend:{display:false},");
        w.write("title:{display:true,text:'" + title + "',color:'#f8fafc',font:{size:14}}},");
        w.write("scales:{x:{ticks:{color:'#94a3b8',maxRotation:40,font:{size:11}}},");
        w.write("y:{ticks:{color:'#94a3b8'},grid:{color:'#334155'}}}}});");
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private static void saveGroup(Path dir, String name, List<String> symbols) throws IOException {
        dir.toFile().mkdirs();
        Path f = dir.resolve(name + ".csv");
        Files.write(f, symbols);
        log.info("  Saved group '{}' → {} stocks", name, symbols.size());
    }

    private static String fmtPnl(double pnl) {
        if (Math.abs(pnl) >= 100_000) return String.format("%.2fL", pnl / 100_000);
        return String.format("%.0f", pnl);
    }

    private static final String CSS = """
            *{margin:0;padding:0;box-sizing:border-box}
            body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                 background:#0f172a;color:#e2e8f0;line-height:1.6;padding:20px}
            .header{text-align:center;padding:30px 20px;margin-bottom:24px;
                    background:linear-gradient(135deg,#1e293b,#334155);border-radius:12px;border:1px solid #475569}
            .header h1{font-size:28px;color:#f8fafc;margin-bottom:8px}
            .subtitle{color:#94a3b8;font-size:14px}
            .callout{background:#1e3a5f;border-left:4px solid #60a5fa;padding:14px 18px;
                     margin-bottom:20px;border-radius:0 8px 8px 0;font-size:14px;line-height:1.9}
            .section{background:#1e293b;border-radius:10px;padding:24px;margin-bottom:20px;border:1px solid #334155}
            .section h2{font-size:18px;color:#f8fafc;margin-bottom:16px;border-bottom:1px solid #334155;padding-bottom:8px}
            table{width:100%;border-collapse:collapse;font-size:12px}
            .stats-table td,.stats-table th{padding:7px 10px;border-bottom:1px solid #334155}
            .stats-table th{text-align:left;color:#94a3b8;font-weight:600;background:#1e293b}
            .stats-table td{text-align:right}
            .stats-table td:first-child{text-align:left}
            .best-row td{color:#4ade80;font-weight:600;background:#1a3a2a}
            .ref-row td{color:#94a3b8;font-style:italic}
            .insight-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}
            .insight{background:#0f172a;border:1px solid #334155;border-radius:8px;padding:16px}
            .insight h3{color:#60a5fa;font-size:14px;margin-bottom:8px}
            .insight p{color:#94a3b8;font-size:13px;line-height:1.6}
            canvas{max-width:100%}
            .footer{text-align:center;color:#475569;font-size:12px;padding:20px}
            """;
}
