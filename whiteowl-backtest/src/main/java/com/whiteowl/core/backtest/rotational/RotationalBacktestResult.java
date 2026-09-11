package com.whiteowl.core.backtest.rotational;

import lombok.Getter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Complete results of a rotational backtest.
 */
@Getter
public final class RotationalBacktestResult {

    private final List<RotationalTrade> tradeLog;
    private final PortfolioTracker portfolio;
    private final RotationalMetrics metrics;

    public RotationalBacktestResult(List<RotationalTrade> tradeLog,
                                    PortfolioTracker portfolio,
                                    RotationalMetrics metrics) {
        this.tradeLog = tradeLog;
        this.portfolio = portfolio;
        this.metrics = metrics;
    }

    /**
     * Export trade log to CSV.
     */
    public void tradeLogToCsv(Path path) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.of("Asia/Kolkata"));

        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("date,symbol,side,entry_price,exit_price,shares,gross_pnl,net_pnl,model_rank,entry_time,or_high,or_low,exit_reason");
            w.newLine();
            for (RotationalTrade t : tradeLog) {
                w.write(String.join(",",
                        fmt.format(Instant.ofEpochMilli(t.date())),
                        t.symbol(),
                        t.side().name(),
                        String.format("%.2f", t.entryPrice()),
                        String.format("%.2f", t.exitPrice()),
                        String.format("%.0f", t.shares()),
                        String.format("%.2f", t.grossPnl()),
                        String.format("%.2f", t.netPnl()),
                        String.format("%.4f", t.modelRank()),
                        t.entryTime() != null ? t.entryTime().toString() : "",
                        String.format("%.2f", t.orHigh()),
                        String.format("%.2f", t.orLow()),
                        t.exitReason().name()));
                w.newLine();
            }
        }
    }

    /**
     * Export equity curve to CSV.
     */
    public void equityCurveToCsv(Path path) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.of("Asia/Kolkata"));

        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("date,equity,daily_pnl,drawdown");
            w.newLine();
            List<Long> dates = portfolio.getDates();
            List<Double> eq = portfolio.getEquityCurve();
            List<Double> pnl = portfolio.getDailyPnl();
            List<Double> dd = portfolio.getDrawdownCurve();
            for (int i = 0; i < dates.size(); i++) {
                w.write(String.join(",",
                        fmt.format(Instant.ofEpochMilli(dates.get(i))),
                        String.format("%.2f", eq.get(i)),
                        String.format("%.2f", pnl.get(i)),
                        String.format("%.4f", dd.get(i))));
                w.newLine();
            }
        }
    }

    @Override
    public String toString() {
        return String.format("""
                === Rotational Backtest Results ===
                Trades: %d
                Final equity: %.2f
                %s""",
                tradeLog.size(), portfolio.getEquity(), metrics);
    }
}
