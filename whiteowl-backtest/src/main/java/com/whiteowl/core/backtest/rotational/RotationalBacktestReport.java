package com.whiteowl.core.backtest.rotational;

import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Comprehensive report data for a rotational backtest.
 * Includes equity curves, trade log, and monthly returns.
 * Can print a full report to the console.
 */
@Getter
@Builder
public final class RotationalBacktestReport {

    private static final Logger log = LoggerFactory.getLogger(RotationalBacktestReport.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RotationalBacktestConfig config;
    private final RotationalMetrics metrics;

    // Equity curves
    private final long[] dates;
    private final double[] equityCurve;
    private final double[] drawdownCurve;
    private final double[] longEquityCurve;
    private final double[] shortEquityCurve;

    // Trade log
    private final List<RotationalTrade> trades;

    // Monthly returns (year -> month -> return), for heatmap
    private final Map<Integer, Map<Integer, Double>> monthlyReturns;

    // Daily PnL
    private final double[] dailyPnl;



    /**
     * Build a report from a backtest result, splitting long/short equity curves.
     */
    public static RotationalBacktestReport fromResult(RotationalBacktestResult result,
                                                       RotationalBacktestConfig config) {
        PortfolioTracker portfolio = result.getPortfolio();
        List<RotationalTrade> trades = result.getTradeLog();

        long[] dates = portfolio.getDates().stream().mapToLong(Long::longValue).toArray();
        double[] equity = portfolio.getEquityCurve().stream().mapToDouble(Double::doubleValue).toArray();
        double[] dd = portfolio.getDrawdownCurve().stream().mapToDouble(Double::doubleValue).toArray();
        double[] dailyPnl = portfolio.getDailyPnl().stream().mapToDouble(Double::doubleValue).toArray();

        // Build long/short equity curves
        double[] longEquity = buildSideEquity(dates, trades, RotationalTrade.Side.LONG, config.getInitialCapital() / 2);
        double[] shortEquity = buildSideEquity(dates, trades, RotationalTrade.Side.SHORT, config.getInitialCapital() / 2);

        // Monthly returns
        Map<Integer, Map<Integer, Double>> monthlyReturns = computeMonthlyReturns(dates, equity);

        return RotationalBacktestReport.builder()
                .config(config)
                .metrics(result.getMetrics())
                .dates(dates)
                .equityCurve(equity)
                .drawdownCurve(dd)
                .longEquityCurve(longEquity)
                .shortEquityCurve(shortEquity)
                .trades(trades)
                .monthlyReturns(monthlyReturns)
                .dailyPnl(dailyPnl)
                .build();
    }

    /**
     * Print the full report to the console via SLF4J logger.
     */
    public void printToConsole() {
        String sep = "══════════════════════════════════════════════════════════════";

        log.info("");
        log.info(sep);
        log.info("  ROTATIONAL ORB BACKTEST REPORT");
        log.info(sep);
        log.info("");

        // ── Configuration ────────────────────────────────────────────
        log.info("── Configuration ───────────────────────────────────────────");
        log.info("{}", config);
        log.info("");

        // ── Summary ──────────────────────────────────────────────────
        log.info("── Summary ─────────────────────────────────────────────────");
        if (dates.length > 0) {
            String startDate = Instant.ofEpochMilli(dates[0]).atZone(IST).toLocalDate().format(DATE_FMT);
            String endDate = Instant.ofEpochMilli(dates[dates.length - 1]).atZone(IST).toLocalDate().format(DATE_FMT);
            log.info("Period:              {} to {}", startDate, endDate);
        }
        log.info("Trading days:        {}", dates.length);
        log.info("Total trades:        {}", metrics.getTotalTrades());

        long longTrades = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG).count();
        long shortTrades = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT).count();
        log.info("  Long trades:       {}", longTrades);
        log.info("  Short trades:      {}", shortTrades);

        long uniqueSymbols = trades.stream().map(RotationalTrade::symbol).distinct().count();
        log.info("Unique symbols:      {}", uniqueSymbols);
        log.info("");

        // ── P&L ──────────────────────────────────────────────────────
        log.info("── P&L ─────────────────────────────────────────────────────");
        double finalEquity = equityCurve.length > 0 ? equityCurve[equityCurve.length - 1] : config.getInitialCapital();
        double totalReturn = (finalEquity - config.getInitialCapital()) / config.getInitialCapital() * 100;
        log.info("Initial capital:     {}", fmt(config.getInitialCapital()));
        log.info("Final capital:       {}", fmt(finalEquity));
        log.info("Net P&L:             {}", fmt(finalEquity - config.getInitialCapital()));
        log.info("Total return:        {}%", fmt(totalReturn));
        log.info("  Long P&L:          {}", fmt(metrics.getLongPnl()));
        log.info("  Short P&L:         {}", fmt(metrics.getShortPnl()));
        log.info("CAGR:                {}%", fmt(metrics.getCagr() * 100));
        log.info("");

        // ── Win/Loss ─────────────────────────────────────────────────
        log.info("── Win/Loss ────────────────────────────────────────────────");
        log.info("Win rate:            {}%", fmt(metrics.getWinRate() * 100));

        long longWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG && t.netPnl() > 0).count();
        long shortWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT && t.netPnl() > 0).count();
        double longWinRate = longTrades > 0 ? (double) longWins / longTrades * 100 : 0;
        double shortWinRate = shortTrades > 0 ? (double) shortWins / shortTrades * 100 : 0;
        log.info("  Long win rate:     {}%  ({}/{})", fmt(longWinRate), longWins, longTrades);
        log.info("  Short win rate:    {}%  ({}/{})", fmt(shortWinRate), shortWins, shortTrades);
        log.info("Profit factor:       {}", fmt(metrics.getProfitFactor()));

        double grossProfit = trades.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
        double grossLoss = trades.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
        long wins = trades.stream().filter(t -> t.netPnl() > 0).count();
        long losses = metrics.getTotalTrades() - wins;
        double avgWin = wins > 0 ? grossProfit / wins : 0;
        double avgLoss = losses > 0 ? grossLoss / losses : 0;
        double expectancy = metrics.getTotalTrades() > 0
                ? (finalEquity - config.getInitialCapital()) / metrics.getTotalTrades() : 0;

        log.info("Avg win:             {}", fmt(avgWin));
        log.info("Avg loss:            {}", fmt(avgLoss));
        log.info("Expectancy/trade:    {}", fmt(expectancy));
        log.info("");

        // ── Exit Breakdown ─────────────────────────────────────────────
        Map<ExitReason, List<RotationalTrade>> byExit = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            byExit.computeIfAbsent(t.exitReason(), k -> new ArrayList<>()).add(t);
        }
        // Only show if there's more than one exit reason (i.e. not all MARKET_CLOSE)
        if (byExit.size() > 1 || !byExit.containsKey(ExitReason.MARKET_CLOSE)) {
            log.info("── Exit Breakdown ──────────────────────────────────────");
            for (Map.Entry<ExitReason, List<RotationalTrade>> entry : byExit.entrySet()) {
                ExitReason reason = entry.getKey();
                List<RotationalTrade> group = entry.getValue();
                long count = group.size();
                double pnl = group.stream().mapToDouble(RotationalTrade::netPnl).sum();
                long groupWins = group.stream().filter(t -> t.netPnl() > 0).count();
                double groupWinRate = count > 0 ? (double) groupWins / count * 100 : 0;
                long groupLong = group.stream().filter(t -> t.side() == RotationalTrade.Side.LONG).count();
                long groupShort = count - groupLong;

                log.info("{}", String.format("%-16s %d / %d (%.1f%%)  P&L: %s  WinRate: %s%%  [L:%d S:%d]",
                        reason.name(), count, metrics.getTotalTrades(),
                        (double) count / metrics.getTotalTrades() * 100,
                        fmt(pnl), fmt(groupWinRate), groupLong, groupShort));
            }
            log.info("");
        }

        // ── Risk ─────────────────────────────────────────────────────
        log.info("── Risk ────────────────────────────────────────────────────");
        log.info("Max drawdown:        {}%", fmt(metrics.getMaxDrawdown() * 100));
        log.info("Sharpe ratio:        {}", fmt(metrics.getSharpe()));
        log.info("Sortino ratio:       {}", fmt(metrics.getSortino()));
        log.info("Daily turnover:      {}%", fmt(metrics.getAvgDailyTurnover() * 100));
        log.info("");

        // ── Monthly Returns ──────────────────────────────────────────
        log.info("── Monthly Returns ─────────────────────────────────────────");
        printMonthlyReturnsTable();
        log.info("");

        // ── Monthly P&L ──────────────────────────────────────────────
        log.info("── Monthly P&L ─────────────────────────────────────────────");
        printMonthlyPnl();
        log.info("");

        // ── Top Winners & Losers ─────────────────────────────────────
        log.info("── Top 10 Winners ──────────────────────────────────────────");
        printTopTrades(10, true);
        log.info("");
        log.info("── Top 10 Losers ───────────────────────────────────────────");
        printTopTrades(10, false);
        log.info("");

        // ── Per-Symbol Summary ───────────────────────────────────────
        log.info("── Per-Symbol Summary (top 20 by trade count) ────────────");
        printPerSymbolSummary(20);
        log.info("");

        log.info(sep);
        log.info("  Report complete.");
        log.info(sep);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private void printMonthlyReturnsTable() {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        StringBuilder header = new StringBuilder(String.format("%-6s", "Year"));
        for (String m : months) header.append(String.format("%8s", m));
        header.append(String.format("%10s", "Annual"));
        log.info("{}", header);

        for (Map.Entry<Integer, Map<Integer, Double>> yearEntry : monthlyReturns.entrySet()) {
            StringBuilder row = new StringBuilder(String.format("%-6d", yearEntry.getKey()));
            double annualReturn = 1.0;
            for (int m = 1; m <= 12; m++) {
                Double ret = yearEntry.getValue().get(m);
                if (ret != null) {
                    row.append(String.format("%7.2f%%", ret * 100));
                    annualReturn *= (1 + ret);
                } else {
                    row.append(String.format("%8s", "-"));
                }
            }
            row.append(String.format("%9.2f%%", (annualReturn - 1) * 100));
            log.info("{}", row);
        }
    }

    private void printMonthlyPnl() {
        Map<YearMonth, Double> monthlyPnl = new TreeMap<>();
        Map<YearMonth, Integer> monthlyTradeCount = new TreeMap<>();
        for (RotationalTrade t : trades) {
            YearMonth ym = YearMonth.from(Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate());
            monthlyPnl.merge(ym, t.netPnl(), Double::sum);
            monthlyTradeCount.merge(ym, 1, Integer::sum);
        }
        log.info("{}", String.format("%-10s %12s %8s", "Month", "Net P&L", "Trades"));
        for (Map.Entry<YearMonth, Double> e : monthlyPnl.entrySet()) {
            log.info("{}", String.format("%-10s %12.2f %8d",
                    e.getKey(), e.getValue(), monthlyTradeCount.get(e.getKey())));
        }
    }

    private void printTopTrades(int n, boolean winners) {
        List<RotationalTrade> sorted = new ArrayList<>(trades);
        if (winners) {
            sorted.sort((a, b) -> Double.compare(b.netPnl(), a.netPnl()));
        } else {
            sorted.sort(Comparator.comparingDouble(RotationalTrade::netPnl));
        }
        log.info("{}", String.format("%-12s %-16s %-6s %10s %10s %10s %8s %12s",
                "Date", "Symbol", "Side", "Entry", "Exit", "OR Range", "Time", "Net P&L"));
        int count = Math.min(n, sorted.size());
        for (int i = 0; i < count; i++) {
            RotationalTrade t = sorted.get(i);
            String dateStr = Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate().format(DATE_FMT);
            String orRange = String.format("%.0f-%.0f", t.orLow(), t.orHigh());
            log.info("{}", String.format("%-12s %-16s %-6s %10.2f %10.2f %10s %8s %12.2f",
                    dateStr, truncate(t.symbol(), 16), t.side(),
                    t.entryPrice(), t.exitPrice(), orRange,
                    t.entryTime() != null ? t.entryTime().toString() : "-",
                    t.netPnl()));
        }
    }

    private void printPerSymbolSummary(int topN) {
        Map<String, List<RotationalTrade>> bySymbol = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            bySymbol.computeIfAbsent(t.symbol(), k -> new ArrayList<>()).add(t);
        }

        log.info("{}", String.format("%-16s %6s %8s %12s %12s %8s",
                "Symbol", "Trades", "Win%", "Total PnL", "Avg PnL", "PF"));

        bySymbol.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(topN)
                .forEach(e -> {
                    List<RotationalTrade> symTrades = e.getValue();
                    int total = symTrades.size();
                    long wins = symTrades.stream().filter(t -> t.netPnl() > 0).count();
                    double totalPnl = symTrades.stream().mapToDouble(RotationalTrade::netPnl).sum();
                    double gp = symTrades.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
                    double gl = symTrades.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
                    double pf = gl > 0 ? gp / gl : 0;
                    log.info("{}", String.format("%-16s %6d %7.1f%% %12.2f %12.2f %8.2f",
                            truncate(e.getKey(), 16), total,
                            (double) wins / total * 100, totalPnl, totalPnl / total, pf));
                });
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    private static double[] buildSideEquity(long[] dates, List<RotationalTrade> trades,
                                            RotationalTrade.Side side, double startingCapital) {
        Map<Long, Double> datePnl = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            if (t.side() == side) {
                datePnl.merge(t.date(), t.netPnl(), Double::sum);
            }
        }

        double[] sideEquity = new double[dates.length];
        double running = startingCapital;
        for (int i = 0; i < dates.length; i++) {
            Double pnl = datePnl.get(dates[i]);
            if (pnl != null) running += pnl;
            sideEquity[i] = running;
        }
        return sideEquity;
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
                    double monthReturn = (equity[i - 1] - monthStartEquity) / monthStartEquity;
                    result.computeIfAbsent(prevYear, k -> new TreeMap<>()).put(prevMonth, monthReturn);
                }
                monthStartEquity = (i > 0) ? equity[i - 1] : equity[0];
                prevYear = year;
                prevMonth = month;
            }
        }

        if (prevYear > 0 && monthStartEquity > 0) {
            double monthReturn = (equity[equity.length - 1] - monthStartEquity) / monthStartEquity;
            result.computeIfAbsent(prevYear, k -> new TreeMap<>()).put(prevMonth, monthReturn);
        }

        return result;
    }
}
