package com.whiteowl.core.backtest.report;

import com.whiteowl.core.backtest.rotational.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a self-contained HTML report for the Composite ORB Strategy.
 * Uses Chart.js (loaded from CDN) for equity curve and drawdown charts.
 * All data is embedded inline — no external files needed.
 */
public final class CompositeHtmlReport {

    private static final Logger log = LoggerFactory.getLogger(CompositeHtmlReport.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private CompositeHtmlReport() {}

    /**
     * Generate a full HTML report and write it to the given path.
     *
     * @param result        backtest result (trade log, portfolio tracker, metrics)
     * @param strategy      composite strategy (sub-strategy configs)
     * @param rankerLabel   name of the ranker used
     * @param allocLabel    name of the allocation mode used
     * @param outputPath    where to write the HTML file
     */
    public static void generate(RotationalBacktestResult result,
                                 CompositeOrbStrategy strategy,
                                 String rankerLabel,
                                 String allocLabel,
                                 Path outputPath) throws IOException {

        PortfolioTracker portfolio = result.getPortfolio();
        List<RotationalTrade> trades = result.getTradeLog();
        RotationalMetrics metrics = result.getMetrics();

        long[] dates = portfolio.getDates().stream().mapToLong(Long::longValue).toArray();
        double[] equity = portfolio.getEquityCurve().stream().mapToDouble(Double::doubleValue).toArray();
        double[] drawdown = portfolio.getDrawdownCurve().stream().mapToDouble(Double::doubleValue).toArray();
        double[] dailyPnl = portfolio.getDailyPnl().stream().mapToDouble(Double::doubleValue).toArray();

        // Build long/short equity curves
        double[] longEquity = buildSideEquity(dates, trades, RotationalTrade.Side.LONG, 0);
        double[] shortEquity = buildSideEquity(dates, trades, RotationalTrade.Side.SHORT, 0);

        // Monthly returns
        Map<Integer, Map<Integer, Double>> monthlyReturns = computeMonthlyReturns(dates, equity);

        // Daily PnL distribution
        double[] dailyReturns = portfolio.dailyReturns();

        // Exit breakdown
        Map<ExitReason, long[]> exitBreakdown = computeExitBreakdown(trades);

        // Per-day trade counts
        int[] longTradesPerDay = new int[dates.length];
        int[] shortTradesPerDay = new int[dates.length];
        Map<Long, Integer> dateIndex = new HashMap<>();
        for (int i = 0; i < dates.length; i++) dateIndex.put(dates[i], i);
        for (RotationalTrade t : trades) {
            Integer idx = dateIndex.get(t.date());
            if (idx != null) {
                if (t.side() == RotationalTrade.Side.LONG) longTradesPerDay[idx]++;
                else shortTradesPerDay[idx]++;
            }
        }

        // Trade stats
        long longCount = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG).count();
        long shortCount = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT).count();
        long wins = trades.stream().filter(t -> t.netPnl() > 0).count();
        long losses = metrics.getTotalTrades() - wins;
        long longWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG && t.netPnl() > 0).count();
        long shortWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT && t.netPnl() > 0).count();
        double grossProfit = trades.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
        double grossLoss = trades.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
        double avgWin = wins > 0 ? grossProfit / wins : 0;
        double avgLoss = losses > 0 ? grossLoss / losses : 0;
        double expectancy = metrics.getTotalTrades() > 0
                ? (metrics.getLongPnl() + metrics.getShortPnl()) / metrics.getTotalTrades() : 0;

        long uniqueSymbols = trades.stream().map(RotationalTrade::symbol).distinct().count();

        // Date labels
        String[] dateLabels = new String[dates.length];
        for (int i = 0; i < dates.length; i++) {
            dateLabels[i] = Instant.ofEpochMilli(dates[i]).atZone(IST).toLocalDate().format(DATE_FMT);
        }

        String startDate = dates.length > 0 ? dateLabels[0] : "N/A";
        String endDate = dates.length > 0 ? dateLabels[dates.length - 1] : "N/A";

        // Cumulative long/short PnL
        double[] cumLongPnl = new double[dates.length];
        double[] cumShortPnl = new double[dates.length];
        Map<Long, Double> longPnlByDate = new LinkedHashMap<>();
        Map<Long, Double> shortPnlByDate = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            if (t.side() == RotationalTrade.Side.LONG) {
                longPnlByDate.merge(t.date(), t.netPnl(), Double::sum);
            } else {
                shortPnlByDate.merge(t.date(), t.netPnl(), Double::sum);
            }
        }
        double runningLong = 0, runningShort = 0;
        for (int i = 0; i < dates.length; i++) {
            runningLong += longPnlByDate.getOrDefault(dates[i], 0.0);
            runningShort += shortPnlByDate.getOrDefault(dates[i], 0.0);
            cumLongPnl[i] = runningLong;
            cumShortPnl[i] = runningShort;
        }

        try (BufferedWriter w = Files.newBufferedWriter(outputPath)) {
            w.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            w.write("<meta charset=\"UTF-8\">\n");
            w.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            w.write("<title>Composite ORB Strategy Report</title>\n");
            w.write("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>\n");
            w.write("<style>\n");
            w.write(CSS);
            w.write("</style>\n</head>\n<body>\n");

            // ── Header ──
            w.write("<div class=\"header\">\n");
            w.write("<h1>Composite ORB Strategy Report</h1>\n");
            w.write(String.format("<p class=\"subtitle\">%s to %s &bull; Ranker: %s &bull; Allocation: %s</p>\n",
                    startDate, endDate, rankerLabel, allocLabel));
            w.write("</div>\n");

            // ── KPI Cards ──
            w.write("<div class=\"kpi-grid\">\n");
            writeKpiCard(w, "CAGR", fmt(metrics.getCagr() * 100) + "%", "green");
            writeKpiCard(w, "Sharpe", fmt(metrics.getSharpe()), "blue");
            writeKpiCard(w, "Sortino", fmt(metrics.getSortino()), "blue");
            writeKpiCard(w, "Calmar", fmt(metrics.getCalmar()), "blue");
            writeKpiCard(w, "Max DD", fmt(metrics.getMaxDrawdown() * 100) + "%", "red");
            writeKpiCard(w, "Net PnL", fmtPnl(metrics.getLongPnl() + metrics.getShortPnl()), "green");
            writeKpiCard(w, "Win Rate", fmt(metrics.getWinRate() * 100) + "%", "neutral");
            writeKpiCard(w, "Profit Factor", fmt(metrics.getProfitFactor()), "neutral");
            w.write("</div>\n");

            // ── Strategy Configuration ──
            w.write("<div class=\"section\">\n<h2>Strategy Configuration</h2>\n");
            w.write("<table class=\"config-table\">\n");
            w.write("<tr><th>Sub-Strategy</th><th>Side</th><th>Gap Dir</th><th>Stop</th><th>Target</th>");
            w.write("<th>Cutoff</th><th>Exit</th><th>Trail</th><th>Gap/ATR</th><th>RVOL</th><th>IBS</th><th>Picks</th></tr>\n");
            for (CompositeOrbStrategy.SubStrategy sub : strategy.getSubStrategies()) {
                RotationalBacktestConfig cfg = sub.config();
                String side = cfg.getSide().name();
                String gapRange = String.format("[%s, %s]",
                        cfg.getMinGapAtr() != null ? fmt(cfg.getMinGapAtr()) : "-",
                        cfg.getMaxGapAtr() != null ? fmt(cfg.getMaxGapAtr()) : "-");
                String rvol = cfg.getMinOrbRvol() != null ? ">=" + fmt(cfg.getMinOrbRvol()) : "-";
                String trail = cfg.isTrailingStopEnabled()
                        ? fmt(cfg.getTrailingStopMultiplier()) + "/" + cfg.getTrailingStopBasis()
                        : "off";
                w.write(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>",
                        sub.name(), side, cfg.getGapDirectionMode(), fmt(cfg.getStopMultiplier()), fmt(cfg.getTargetMultiplier())));
                w.write(String.format("<td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>&le;%s</td><td>%d</td></tr>\n",
                        cfg.getEntryCutoffTime(),
                        cfg.getExitTime() != null ? cfg.getExitTime() : "MktClose",
                        trail, gapRange, rvol,
                        cfg.getMaxOrbIbs() != null ? fmt(cfg.getMaxOrbIbs()) : "-",
                        cfg.getPicks()));
            }
            w.write("</table>\n</div>\n");

            // ── Summary Stats ──
            w.write("<div class=\"section\">\n<h2>Performance Summary</h2>\n");
            w.write("<div class=\"stats-grid\">\n");
            w.write("<div class=\"stats-col\">\n<h3>Returns</h3>\n<table class=\"stats-table\">\n");
            w.write(statRow("Initial Capital", fmt(strategy.getInitialCapital())));
            w.write(statRow("Final Equity", fmt(equity.length > 0 ? equity[equity.length - 1] : strategy.getInitialCapital())));
            w.write(statRow("Net P&L", fmtPnl(metrics.getLongPnl() + metrics.getShortPnl())));
            w.write(statRow("Long P&L", fmtPnl(metrics.getLongPnl())));
            w.write(statRow("Short P&L", fmtPnl(metrics.getShortPnl())));
            w.write(statRow("CAGR", fmt(metrics.getCagr() * 100) + "%"));
            w.write(statRow("Total Return", fmt((equity.length > 0 ? equity[equity.length-1] : strategy.getInitialCapital()) / strategy.getInitialCapital() * 100 - 100) + "%"));
            w.write("</table>\n</div>\n");

            w.write("<div class=\"stats-col\">\n<h3>Risk</h3>\n<table class=\"stats-table\">\n");
            w.write(statRow("Sharpe Ratio", fmt(metrics.getSharpe())));
            w.write(statRow("Sortino Ratio", fmt(metrics.getSortino())));
            w.write(statRow("Calmar Ratio", fmt(metrics.getCalmar())));
            w.write(statRow("Max Drawdown", fmt(metrics.getMaxDrawdown() * 100) + "%"));
            w.write(statRow("Profit Factor", fmt(metrics.getProfitFactor())));
            w.write(statRow("Avg Daily Turnover", fmt(metrics.getAvgDailyTurnover() * 100) + "%"));
            w.write("</table>\n</div>\n");

            w.write("<div class=\"stats-col\">\n<h3>Trades</h3>\n<table class=\"stats-table\">\n");
            w.write(statRow("Total Trades", String.valueOf(metrics.getTotalTrades())));
            w.write(statRow("Long Trades", String.valueOf(longCount)));
            w.write(statRow("Short Trades", String.valueOf(shortCount)));
            w.write(statRow("Win Rate", fmt(metrics.getWinRate() * 100) + "%"));
            w.write(statRow("Long Win Rate", fmt(longCount > 0 ? (double) longWins / longCount * 100 : 0) + "%"));
            w.write(statRow("Short Win Rate", fmt(shortCount > 0 ? (double) shortWins / shortCount * 100 : 0) + "%"));
            w.write(statRow("Avg Win", fmt(avgWin)));
            w.write(statRow("Avg Loss", fmt(avgLoss)));
            w.write(statRow("Expectancy / Trade", fmt(expectancy)));
            w.write(statRow("Unique Symbols", String.valueOf(uniqueSymbols)));
            w.write(statRow("Trading Days", String.valueOf(dates.length)));
            w.write("</table>\n</div>\n</div>\n</div>\n");

            // ── Equity Curve Chart ──
            w.write("<div class=\"section\">\n<h2>Equity Curve</h2>\n");
            w.write("<canvas id=\"equityChart\" height=\"80\"></canvas>\n</div>\n");

            // ── Drawdown Chart ──
            w.write("<div class=\"section\">\n<h2>Drawdown</h2>\n");
            w.write("<canvas id=\"drawdownChart\" height=\"50\"></canvas>\n</div>\n");

            // ── Long vs Short PnL ──
            w.write("<div class=\"section\">\n<h2>Cumulative Long vs Short P&L</h2>\n");
            w.write("<canvas id=\"longShortChart\" height=\"60\"></canvas>\n</div>\n");

            // ── Daily PnL Chart ──
            w.write("<div class=\"section\">\n<h2>Daily P&L</h2>\n");
            w.write("<canvas id=\"dailyPnlChart\" height=\"60\"></canvas>\n</div>\n");

            // ── Daily Returns Distribution ──
            w.write("<div class=\"section\">\n<h2>Daily Returns Distribution</h2>\n");
            w.write("<canvas id=\"histChart\" height=\"60\"></canvas>\n</div>\n");

            // ── Exit Breakdown ──
            w.write("<div class=\"section\">\n<h2>Exit Breakdown</h2>\n");
            w.write("<div class=\"exit-grid\">\n");
            w.write("<div><canvas id=\"exitPieChart\" height=\"200\"></canvas></div>\n");
            w.write("<div>\n<table class=\"stats-table\">\n");
            w.write("<tr><th>Exit Reason</th><th>Count</th><th>%</th><th>Net P&L</th><th>Win%</th></tr>\n");
            for (Map.Entry<ExitReason, long[]> e : exitBreakdown.entrySet()) {
                long[] v = e.getValue();
                double pnl = exitPnl(trades, e.getKey());
                long cnt = v[0];
                double pct = metrics.getTotalTrades() > 0 ? (double) cnt / metrics.getTotalTrades() * 100 : 0;
                double wr = cnt > 0 ? (double) v[1] / cnt * 100 : 0;
                w.write(String.format("<tr><td>%s</td><td>%d</td><td>%.1f%%</td><td>%s</td><td>%.1f%%</td></tr>\n",
                        e.getKey().name(), cnt, pct, fmtPnl(pnl), wr));
            }
            w.write("</table>\n</div>\n</div>\n</div>\n");

            // ── Monthly Returns Heatmap ──
            w.write("<div class=\"section\">\n<h2>Monthly Returns</h2>\n");
            writeMonthlyReturnsTable(w, monthlyReturns);
            w.write("</div>\n");

            // ── Monthly P&L Table ──
            w.write("<div class=\"section\">\n<h2>Monthly P&L</h2>\n");
            writeMonthlyPnlTable(w, trades);
            w.write("</div>\n");

            // ── Top Winners & Losers ──
            w.write("<div class=\"section\">\n<h2>Top 15 Winners</h2>\n");
            writeTopTrades(w, trades, 15, true);
            w.write("</div>\n");
            w.write("<div class=\"section\">\n<h2>Top 15 Losers</h2>\n");
            writeTopTrades(w, trades, 15, false);
            w.write("</div>\n");

            // ── Per-Symbol Summary ──
            w.write("<div class=\"section\">\n<h2>Per-Symbol Summary (top 25 by trade count)</h2>\n");
            writePerSymbolSummary(w, trades, 25);
            w.write("</div>\n");

            // ── JavaScript / Charts ──
            w.write("<script>\n");
            writeChartData(w, dateLabels, equity, drawdown, dailyPnl, cumLongPnl, cumShortPnl,
                    dailyReturns, exitBreakdown, metrics.getTotalTrades());
            w.write("</script>\n");

            w.write("<div class=\"footer\">Generated on " +
                    LocalDateTime.now(IST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " IST</div>\n");
            w.write("</body>\n</html>\n");
        }

        log.info("HTML report written to: {}", outputPath.toAbsolutePath());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Chart JS generation
    // ═══════════════════════════════════════════════════════════════════

    private static void writeChartData(BufferedWriter w, String[] labels, double[] equity,
                                        double[] drawdown, double[] dailyPnl,
                                        double[] cumLongPnl, double[] cumShortPnl,
                                        double[] dailyReturns,
                                        Map<ExitReason, long[]> exitBreakdown,
                                        int totalTrades) throws IOException {
        String lblJson = toJsonArray(labels);
        String eqJson = toJsonArray(equity);
        String ddJson = toJsonArrayPct(drawdown);
        String dpJson = toJsonArray(dailyPnl);
        String clJson = toJsonArray(cumLongPnl);
        String csJson = toJsonArray(cumShortPnl);

        // Equity chart
        w.write("new Chart(document.getElementById('equityChart'),{type:'line',data:{labels:" + lblJson + ",datasets:[");
        w.write("{label:'Portfolio Equity',data:" + eqJson + ",borderColor:'#2563eb',backgroundColor:'rgba(37,99,235,0.08)',fill:true,pointRadius:0,borderWidth:2}");
        w.write("]},options:{responsive:true,interaction:{intersect:false,mode:'index'},plugins:{legend:{display:true}},scales:{y:{title:{display:true,text:'Equity (INR)'}},x:{ticks:{maxTicksToShow:12,autoSkip:true,maxRotation:45}}}}});\n");

        // Drawdown chart
        w.write("new Chart(document.getElementById('drawdownChart'),{type:'line',data:{labels:" + lblJson + ",datasets:[");
        w.write("{label:'Drawdown %',data:" + ddJson + ",borderColor:'#dc2626',backgroundColor:'rgba(220,38,38,0.12)',fill:true,pointRadius:0,borderWidth:1.5}");
        w.write("]},options:{responsive:true,interaction:{intersect:false,mode:'index'},plugins:{legend:{display:false}},scales:{y:{title:{display:true,text:'Drawdown %'},reverse:false},x:{ticks:{maxTicksToShow:12,autoSkip:true,maxRotation:45}}}}});\n");

        // Long vs Short PnL
        w.write("new Chart(document.getElementById('longShortChart'),{type:'line',data:{labels:" + lblJson + ",datasets:[");
        w.write("{label:'Long PnL',data:" + clJson + ",borderColor:'#16a34a',pointRadius:0,borderWidth:2},");
        w.write("{label:'Short PnL',data:" + csJson + ",borderColor:'#dc2626',pointRadius:0,borderWidth:2}");
        w.write("]},options:{responsive:true,interaction:{intersect:false,mode:'index'},plugins:{legend:{display:true}},scales:{y:{title:{display:true,text:'Cumulative PnL (INR)'}},x:{ticks:{maxTicksToShow:12,autoSkip:true,maxRotation:45}}}}});\n");

        // Daily PnL bar chart
        String posColors = buildColorArray(dailyPnl, "'rgba(22,163,74,0.7)'", "'rgba(220,38,38,0.7)'");
        w.write("new Chart(document.getElementById('dailyPnlChart'),{type:'bar',data:{labels:" + lblJson + ",datasets:[");
        w.write("{label:'Daily PnL',data:" + dpJson + ",backgroundColor:" + posColors + "}");
        w.write("]},options:{responsive:true,plugins:{legend:{display:false}},scales:{y:{title:{display:true,text:'PnL (INR)'}},x:{ticks:{maxTicksToShow:12,autoSkip:true,maxRotation:45}}}}});\n");

        // Daily returns histogram
        writeHistogramChart(w, dailyReturns);

        // Exit reason pie
        writeExitPieChart(w, exitBreakdown, totalTrades);
    }

    private static void writeHistogramChart(BufferedWriter w, double[] returns) throws IOException {
        // Build bins from -4% to +4% in 0.25% increments
        int numBins = 33;
        double binWidth = 0.0025;
        double minBin = -0.04;
        int[] counts = new int[numBins];
        for (double r : returns) {
            int idx = (int) Math.floor((r - minBin) / binWidth);
            if (idx < 0) idx = 0;
            if (idx >= numBins) idx = numBins - 1;
            counts[idx]++;
        }
        StringBuilder labels = new StringBuilder("[");
        StringBuilder data = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");
        for (int i = 0; i < numBins; i++) {
            double center = minBin + binWidth * i + binWidth / 2;
            if (i > 0) { labels.append(","); data.append(","); colors.append(","); }
            labels.append(String.format("'%.2f%%'", center * 100));
            data.append(counts[i]);
            colors.append(center >= 0 ? "'rgba(22,163,74,0.7)'" : "'rgba(220,38,38,0.7)'");
        }
        labels.append("]"); data.append("]"); colors.append("]");

        w.write("new Chart(document.getElementById('histChart'),{type:'bar',data:{labels:" + labels + ",datasets:[");
        w.write("{label:'Frequency',data:" + data + ",backgroundColor:" + colors + "}");
        w.write("]},options:{responsive:true,plugins:{legend:{display:false}},scales:{y:{title:{display:true,text:'Frequency'}},x:{title:{display:true,text:'Daily Return'}}}}});\n");
    }

    private static void writeExitPieChart(BufferedWriter w, Map<ExitReason, long[]> exitBreakdown,
                                           int totalTrades) throws IOException {
        StringBuilder labels = new StringBuilder("[");
        StringBuilder data = new StringBuilder("[");
        String[] pieColors = {"'#2563eb'", "'#16a34a'", "'#f59e0b'", "'#dc2626'", "'#8b5cf6'"};
        StringBuilder colors = new StringBuilder("[");
        int i = 0;
        for (Map.Entry<ExitReason, long[]> e : exitBreakdown.entrySet()) {
            if (i > 0) { labels.append(","); data.append(","); colors.append(","); }
            labels.append("'").append(e.getKey().name()).append("'");
            data.append(e.getValue()[0]);
            colors.append(pieColors[i % pieColors.length]);
            i++;
        }
        labels.append("]"); data.append("]"); colors.append("]");

        w.write("new Chart(document.getElementById('exitPieChart'),{type:'doughnut',data:{labels:" + labels + ",datasets:[");
        w.write("{data:" + data + ",backgroundColor:" + colors + "}");
        w.write("]},options:{responsive:true,plugins:{legend:{position:'bottom'}}}});\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HTML table generators
    // ═══════════════════════════════════════════════════════════════════

    private static void writeMonthlyReturnsTable(BufferedWriter w,
                                                  Map<Integer, Map<Integer, Double>> monthlyReturns) throws IOException {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        w.write("<table class=\"monthly-table\">\n<tr><th>Year</th>");
        for (String m : months) w.write("<th>" + m + "</th>");
        w.write("<th>Annual</th></tr>\n");

        for (Map.Entry<Integer, Map<Integer, Double>> yearEntry : monthlyReturns.entrySet()) {
            w.write("<tr><td><strong>" + yearEntry.getKey() + "</strong></td>");
            double annualReturn = 1.0;
            for (int m = 1; m <= 12; m++) {
                Double ret = yearEntry.getValue().get(m);
                if (ret != null) {
                    double pct = ret * 100;
                    String cls = pct >= 0 ? "pos" : "neg";
                    w.write(String.format("<td class=\"%s\">%.2f%%</td>", cls, pct));
                    annualReturn *= (1 + ret);
                } else {
                    w.write("<td class=\"na\">-</td>");
                }
            }
            double annPct = (annualReturn - 1) * 100;
            String cls = annPct >= 0 ? "pos" : "neg";
            w.write(String.format("<td class=\"%s\"><strong>%.2f%%</strong></td></tr>\n", cls, annPct));
        }
        w.write("</table>\n");
    }

    private static void writeMonthlyPnlTable(BufferedWriter w, List<RotationalTrade> trades) throws IOException {
        Map<YearMonth, double[]> monthlyStats = new TreeMap<>();
        for (RotationalTrade t : trades) {
            YearMonth ym = YearMonth.from(Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate());
            double[] stats = monthlyStats.computeIfAbsent(ym, k -> new double[3]); // [pnl, tradeCount, wins]
            stats[0] += t.netPnl();
            stats[1]++;
            if (t.netPnl() > 0) stats[2]++;
        }
        w.write("<table class=\"stats-table\">\n");
        w.write("<tr><th>Month</th><th>Net P&L</th><th>Trades</th><th>Win%</th></tr>\n");
        for (Map.Entry<YearMonth, double[]> e : monthlyStats.entrySet()) {
            double[] s = e.getValue();
            String cls = s[0] >= 0 ? "pos" : "neg";
            w.write(String.format("<tr><td>%s</td><td class=\"%s\">%s</td><td>%.0f</td><td>%.1f%%</td></tr>\n",
                    e.getKey(), cls, fmtPnl(s[0]), s[1], s[1] > 0 ? s[2] / s[1] * 100 : 0));
        }
        w.write("</table>\n");
    }

    private static void writeTopTrades(BufferedWriter w, List<RotationalTrade> trades,
                                        int n, boolean winners) throws IOException {
        List<RotationalTrade> sorted = new ArrayList<>(trades);
        if (winners) sorted.sort((a, b) -> Double.compare(b.netPnl(), a.netPnl()));
        else sorted.sort(Comparator.comparingDouble(RotationalTrade::netPnl));

        w.write("<table class=\"stats-table\">\n");
        w.write("<tr><th>Date</th><th>Symbol</th><th>Side</th><th>Entry</th><th>Exit</th><th>Shares</th><th>Net PnL</th><th>Exit Reason</th></tr>\n");
        int count = Math.min(n, sorted.size());
        for (int i = 0; i < count; i++) {
            RotationalTrade t = sorted.get(i);
            String dateStr = Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate().format(DATE_FMT);
            String cls = t.netPnl() >= 0 ? "pos" : "neg";
            w.write(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%.2f</td><td>%.2f</td><td>%.0f</td><td class=\"%s\">%.2f</td><td>%s</td></tr>\n",
                    dateStr, t.symbol(), t.side(), t.entryPrice(), t.exitPrice(), t.shares(), cls, t.netPnl(), t.exitReason()));
        }
        w.write("</table>\n");
    }

    private static void writePerSymbolSummary(BufferedWriter w, List<RotationalTrade> trades, int topN) throws IOException {
        Map<String, List<RotationalTrade>> bySymbol = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            bySymbol.computeIfAbsent(t.symbol(), k -> new ArrayList<>()).add(t);
        }

        w.write("<table class=\"stats-table\">\n");
        w.write("<tr><th>Symbol</th><th>Trades</th><th>Win%</th><th>Total PnL</th><th>Avg PnL</th><th>PF</th></tr>\n");

        bySymbol.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(topN)
                .forEach(e -> {
                    try {
                        List<RotationalTrade> symTrades = e.getValue();
                        int total = symTrades.size();
                        long symWins = symTrades.stream().filter(t -> t.netPnl() > 0).count();
                        double totalPnl = symTrades.stream().mapToDouble(RotationalTrade::netPnl).sum();
                        double gp = symTrades.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
                        double gl = symTrades.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
                        double pf = gl > 0 ? gp / gl : 0;
                        String cls = totalPnl >= 0 ? "pos" : "neg";
                        w.write(String.format("<tr><td>%s</td><td>%d</td><td>%.1f%%</td><td class=\"%s\">%s</td><td>%.2f</td><td>%.2f</td></tr>\n",
                                e.getKey(), total, (double) symWins / total * 100, cls, fmtPnl(totalPnl), totalPnl / total, pf));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        w.write("</table>\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private static void writeKpiCard(BufferedWriter w, String label, String value, String color) throws IOException {
        w.write(String.format("<div class=\"kpi-card %s\"><div class=\"kpi-value\">%s</div><div class=\"kpi-label\">%s</div></div>\n",
                color, value, label));
    }

    private static String statRow(String label, String value) {
        return String.format("<tr><td>%s</td><td><strong>%s</strong></td></tr>\n", label, value);
    }

    private static Map<ExitReason, long[]> computeExitBreakdown(List<RotationalTrade> trades) {
        Map<ExitReason, long[]> map = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            long[] counts = map.computeIfAbsent(t.exitReason(), k -> new long[2]); // [count, wins]
            counts[0]++;
            if (t.netPnl() > 0) counts[1]++;
        }
        return map;
    }

    private static double exitPnl(List<RotationalTrade> trades, ExitReason reason) {
        return trades.stream().filter(t -> t.exitReason() == reason).mapToDouble(RotationalTrade::netPnl).sum();
    }

    private static double[] buildSideEquity(long[] dates, List<RotationalTrade> trades,
                                             RotationalTrade.Side side, double start) {
        Map<Long, Double> datePnl = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            if (t.side() == side) datePnl.merge(t.date(), t.netPnl(), Double::sum);
        }
        double[] eq = new double[dates.length];
        double running = start;
        for (int i = 0; i < dates.length; i++) {
            Double pnl = datePnl.get(dates[i]);
            if (pnl != null) running += pnl;
            eq[i] = running;
        }
        return eq;
    }

    private static Map<Integer, Map<Integer, Double>> computeMonthlyReturns(long[] dates, double[] equity) {
        Map<Integer, Map<Integer, Double>> result = new TreeMap<>();
        if (dates.length < 2) return result;
        int prevMonth = -1, prevYear = -1;
        double monthStartEquity = equity[0];
        for (int i = 0; i < dates.length; i++) {
            LocalDate ld = Instant.ofEpochMilli(dates[i]).atZone(IST).toLocalDate();
            int year = ld.getYear();
            int month = ld.getMonthValue();
            if (year != prevYear || month != prevMonth) {
                if (prevYear > 0 && i > 0 && monthStartEquity > 0) {
                    double ret = (equity[i - 1] - monthStartEquity) / monthStartEquity;
                    result.computeIfAbsent(prevYear, k -> new TreeMap<>()).put(prevMonth, ret);
                }
                monthStartEquity = (i > 0) ? equity[i - 1] : equity[0];
                prevYear = year;
                prevMonth = month;
            }
        }
        if (prevYear > 0 && monthStartEquity > 0) {
            double ret = (equity[equity.length - 1] - monthStartEquity) / monthStartEquity;
            result.computeIfAbsent(prevYear, k -> new TreeMap<>()).put(prevMonth, ret);
        }
        return result;
    }

    private static String toJsonArray(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("'").append(arr[i]).append("'");
        }
        return sb.append("]").toString();
    }

    private static String toJsonArray(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.2f", arr[i]));
        }
        return sb.append("]").toString();
    }

    private static String toJsonArrayPct(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.4f", arr[i] * 100));
        }
        return sb.append("]").toString();
    }

    private static String buildColorArray(double[] values, String posColor, String negColor) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i] >= 0 ? posColor : negColor);
        }
        return sb.append("]").toString();
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    private static String fmtPnl(double pnl) {
        if (Math.abs(pnl) >= 100_000) return String.format("%.2fL", pnl / 100_000);
        return String.format("%.0f", pnl);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CSS
    // ═══════════════════════════════════════════════════════════════════

    private static final String CSS = """
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                background: #0f172a; color: #e2e8f0; line-height: 1.6; padding: 20px;
            }
            .header {
                text-align: center; padding: 30px 20px; margin-bottom: 24px;
                background: linear-gradient(135deg, #1e293b, #334155); border-radius: 12px;
                border: 1px solid #475569;
            }
            .header h1 { font-size: 28px; color: #f8fafc; margin-bottom: 8px; }
            .subtitle { color: #94a3b8; font-size: 14px; }
            .kpi-grid {
                display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                gap: 16px; margin-bottom: 24px;
            }
            .kpi-card {
                background: #1e293b; border-radius: 10px; padding: 20px; text-align: center;
                border: 1px solid #334155; transition: transform 0.2s;
            }
            .kpi-card:hover { transform: translateY(-2px); }
            .kpi-value { font-size: 26px; font-weight: 700; }
            .kpi-label { font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-top: 4px; }
            .kpi-card.green .kpi-value { color: #4ade80; }
            .kpi-card.red .kpi-value { color: #f87171; }
            .kpi-card.blue .kpi-value { color: #60a5fa; }
            .kpi-card.neutral .kpi-value { color: #e2e8f0; }
            .section {
                background: #1e293b; border-radius: 10px; padding: 24px; margin-bottom: 20px;
                border: 1px solid #334155;
            }
            .section h2 { font-size: 18px; color: #f8fafc; margin-bottom: 16px; border-bottom: 1px solid #334155; padding-bottom: 8px; }
            .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }
            .stats-col h3 { font-size: 14px; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }
            table { width: 100%; border-collapse: collapse; font-size: 13px; }
            .stats-table td, .stats-table th { padding: 6px 10px; border-bottom: 1px solid #334155; }
            .stats-table th { text-align: left; color: #94a3b8; font-weight: 600; }
            .stats-table td:last-child { text-align: right; }
            .config-table td, .config-table th { padding: 8px 10px; border-bottom: 1px solid #334155; text-align: center; font-size: 12px; }
            .config-table th { color: #94a3b8; font-weight: 600; }
            .monthly-table td, .monthly-table th { padding: 6px 8px; text-align: center; font-size: 12px; border-bottom: 1px solid #334155; }
            .monthly-table th { color: #94a3b8; font-weight: 600; }
            .pos { color: #4ade80; }
            .neg { color: #f87171; }
            .na { color: #475569; }
            .exit-grid { display: grid; grid-template-columns: 280px 1fr; gap: 20px; align-items: start; }
            canvas { max-width: 100%; }
            .footer { text-align: center; color: #475569; font-size: 12px; padding: 20px; }
            @media (max-width: 768px) {
                .stats-grid { grid-template-columns: 1fr; }
                .exit-grid { grid-template-columns: 1fr; }
                .kpi-grid { grid-template-columns: repeat(2, 1fr); }
            }
            """;
}
