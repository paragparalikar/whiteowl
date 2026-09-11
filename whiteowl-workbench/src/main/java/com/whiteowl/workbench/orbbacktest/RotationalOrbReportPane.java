package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.ExitReason;
import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.rotational.RotationalBacktestReport;
import com.whiteowl.core.backtest.rotational.RotationalMetrics;
import com.whiteowl.core.backtest.rotational.RotationalTrade;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Left-side report pane showing backtest metrics in card layout.
 * Follows the same pattern as {@code ReportPane} in the v2 backtest module.
 */
public final class RotationalOrbReportPane extends ScrollPane {

    private static final String SCROLL_STYLE = "rot-orb-report-scroll";
    private static final String CONTENT_STYLE = "rot-orb-report-content";
    private static final String SECTION_CARD_STYLE = "rot-orb-report-card";
    private static final String SECTION_HEADER_STYLE = "rot-orb-report-section-header";
    private static final String METRIC_LABEL_STYLE = "rot-orb-report-metric-label";
    private static final String METRIC_VALUE_STYLE = "rot-orb-report-metric-value";
    private static final String VALUE_POSITIVE_STYLE = "rot-orb-report-value-positive";
    private static final String VALUE_NEGATIVE_STYLE = "rot-orb-report-value-negative";
    private static final String VALUE_NEUTRAL_STYLE = "rot-orb-report-value-neutral";

    private static final String PERFORMANCE_HEADER = "Performance";
    private static final String RISK_HEADER = "Risk";
    private static final String TRADES_HEADER = "Trades";
    private static final String EXIT_BREAKDOWN_HEADER = "Exit Breakdown";
    private static final String CONFIG_HEADER = "Configuration";

    private static final int GRID_V_GAP = 1;
    private static final int CONTENT_SPACING = 8;
    private static final int CONTENT_PADDING = 10;
    private static final int CARD_PADDING = 8;
    private static final double MILLION = 1_000_000;
    private static final double THOUSAND = 1_000;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final VBox content;

    public RotationalOrbReportPane() {
        this.content = new VBox(CONTENT_SPACING);
        getStyleClass().add(SCROLL_STYLE);
        content.getStyleClass().add(CONTENT_STYLE);
        content.setPadding(new Insets(CONTENT_PADDING));
        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    }

    public void setReport(RotationalBacktestReport report) {
        content.getChildren().clear();
        if (report == null) return;

        content.getChildren().addAll(
                buildPerformanceCard(report),
                buildRiskCard(report),
                buildTradesCard(report),
                buildExitBreakdownCard(report),
                buildConfigCard(report)
        );
    }

    // ── Performance Card ─────────────────────────────────────────────

    private VBox buildPerformanceCard(RotationalBacktestReport report) {
        RotationalMetrics m = report.getMetrics();
        RotationalBacktestConfig c = report.getConfig();
        double[] equity = report.getEquityCurve();
        long[] dates = report.getDates();

        double finalEquity = equity.length > 0 ? equity[equity.length - 1] : c.getInitialCapital();
        double netPnl = finalEquity - c.getInitialCapital();
        double totalReturn = (c.getInitialCapital() > 0) ? netPnl / c.getInitialCapital() * 100 : 0;

        GridPane grid = createMetricsGrid();
        int row = 0;
        if (dates.length > 0) {
            String startDate = Instant.ofEpochMilli(dates[0]).atZone(IST).toLocalDate().format(DATE_FMT);
            String endDate = Instant.ofEpochMilli(dates[dates.length - 1]).atZone(IST).toLocalDate().format(DATE_FMT);
            addRow(grid, row++, "Period", startDate + " to " + endDate, VALUE_NEUTRAL_STYLE);
            addRow(grid, row++, "Trading Days", String.valueOf(dates.length), VALUE_NEUTRAL_STYLE);
        }
        addRow(grid, row++, "Initial Capital", formatCurrency(c.getInitialCapital()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Final Capital", formatCurrency(finalEquity), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Net P&L", formatCurrency(netPnl), colorForSign(netPnl));
        addRow(grid, row++, "Total Return", formatPercent(totalReturn), colorForSign(totalReturn));
        addRow(grid, row++, "Long P&L", formatCurrency(m.getLongPnl()), colorForSign(m.getLongPnl()));
        addRow(grid, row++, "Short P&L", formatCurrency(m.getShortPnl()), colorForSign(m.getShortPnl()));
        addRow(grid, row, "CAGR", formatPercent(m.getCagr() * 100), colorForSign(m.getCagr()));
        return buildCard(PERFORMANCE_HEADER, grid);
    }

    // ── Risk Card ────────────────────────────────────────────────────

    private VBox buildRiskCard(RotationalBacktestReport report) {
        RotationalMetrics m = report.getMetrics();
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Max Drawdown", formatPercent(m.getMaxDrawdown() * 100), VALUE_NEGATIVE_STYLE);
        addRow(grid, row++, "Sharpe Ratio", formatRatio(m.getSharpe()), colorForSign(m.getSharpe()));
        addRow(grid, row++, "Sortino Ratio", formatRatio(m.getSortino()), colorForSign(m.getSortino()));
        addRow(grid, row++, "Calmar Ratio", formatRatio(m.getCalmar()), colorForSign(m.getCalmar()));
        addRow(grid, row, "Avg Daily Turnover", formatPercent(m.getAvgDailyTurnover() * 100), VALUE_NEUTRAL_STYLE);
        return buildCard(RISK_HEADER, grid);
    }

    // ── Trades Card ──────────────────────────────────────────────────

    private VBox buildTradesCard(RotationalBacktestReport report) {
        RotationalMetrics m = report.getMetrics();
        List<RotationalTrade> trades = report.getTrades();

        long longTrades = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG).count();
        long shortTrades = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT).count();
        long longWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.LONG && t.netPnl() > 0).count();
        long shortWins = trades.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT && t.netPnl() > 0).count();
        double longWinRate = longTrades > 0 ? (double) longWins / longTrades * 100 : 0;
        double shortWinRate = shortTrades > 0 ? (double) shortWins / shortTrades * 100 : 0;

        long wins = trades.stream().filter(t -> t.netPnl() > 0).count();
        long losses = m.getTotalTrades() - wins;
        double grossProfit = trades.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
        double grossLoss = trades.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
        double avgWin = wins > 0 ? grossProfit / wins : 0;
        double avgLoss = losses > 0 ? grossLoss / losses : 0;
        long uniqueSymbols = trades.stream().map(RotationalTrade::symbol).distinct().count();

        double finalEquity = report.getEquityCurve().length > 0
                ? report.getEquityCurve()[report.getEquityCurve().length - 1]
                : report.getConfig().getInitialCapital();
        double expectancy = m.getTotalTrades() > 0
                ? (finalEquity - report.getConfig().getInitialCapital()) / m.getTotalTrades() : 0;

        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Total Trades", String.valueOf(m.getTotalTrades()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "  Long Trades", String.valueOf(longTrades), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "  Short Trades", String.valueOf(shortTrades), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Unique Symbols", String.valueOf(uniqueSymbols), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Win Rate", formatPercent(m.getWinRate() * 100), colorForWinRate(m.getWinRate()));
        addRow(grid, row++, "  Long Win Rate", formatPercent(longWinRate), colorForWinRate(longWinRate / 100));
        addRow(grid, row++, "  Short Win Rate", formatPercent(shortWinRate), colorForWinRate(shortWinRate / 100));
        addRow(grid, row++, "Profit Factor", formatRatio(m.getProfitFactor()), colorForThreshold(m.getProfitFactor(), 1.0));
        addRow(grid, row++, "Avg Win", formatCurrency(avgWin), VALUE_POSITIVE_STYLE);
        addRow(grid, row++, "Avg Loss", formatCurrency(avgLoss), VALUE_NEGATIVE_STYLE);
        addRow(grid, row, "Expectancy/Trade", formatCurrency(expectancy), colorForSign(expectancy));
        return buildCard(TRADES_HEADER, grid);
    }

    // ── Exit Breakdown Card ──────────────────────────────────────────

    private VBox buildExitBreakdownCard(RotationalBacktestReport report) {
        List<RotationalTrade> trades = report.getTrades();
        int totalTrades = report.getMetrics().getTotalTrades();

        Map<ExitReason, List<RotationalTrade>> byExit = new LinkedHashMap<>();
        for (RotationalTrade t : trades) {
            byExit.computeIfAbsent(t.exitReason(), k -> new ArrayList<>()).add(t);
        }

        GridPane grid = createMetricsGrid();
        int row = 0;

        for (Map.Entry<ExitReason, List<RotationalTrade>> entry : byExit.entrySet()) {
            ExitReason reason = entry.getKey();
            List<RotationalTrade> group = entry.getValue();
            int count = group.size();
            double pnl = group.stream().mapToDouble(RotationalTrade::netPnl).sum();
            long groupWins = group.stream().filter(t -> t.netPnl() > 0).count();
            double groupWinRate = count > 0 ? (double) groupWins / count * 100 : 0;
            double pct = totalTrades > 0 ? (double) count / totalTrades * 100 : 0;

            addRow(grid, row++, reason.name(), "", VALUE_NEUTRAL_STYLE);
            addRow(grid, row++, "  Count", String.format("%d (%.1f%%)", count, pct), VALUE_NEUTRAL_STYLE);
            addRow(grid, row++, "  P&L", formatCurrency(pnl), colorForSign(pnl));
            addRow(grid, row++, "  Win Rate", formatPercent(groupWinRate), colorForWinRate(groupWinRate / 100));
        }
        return buildCard(EXIT_BREAKDOWN_HEADER, grid);
    }

    // ── Config Card ──────────────────────────────────────────────────

    private VBox buildConfigCard(RotationalBacktestReport report) {
        RotationalBacktestConfig c = report.getConfig();
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Universe", c.getUniverseGroupName(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "OR Minutes", String.valueOf(c.getOpeningRangeMinutes()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Entry Method", c.getEntryMethod().name(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Ranker", c.getRankerType().name(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Side", c.getSide().name(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Picks", String.valueOf(c.getPicks()), VALUE_NEUTRAL_STYLE);
        if (c.getMinGapAtr() != null) {
            addRow(grid, row++, "Gap/ATR", String.format("%.2f", c.getMinGapAtr()), VALUE_NEUTRAL_STYLE);
        }
        addRow(grid, row++, "Stop", String.format("%.2fx %s", c.getStopMultiplier(), c.getStopBasis()), VALUE_NEUTRAL_STYLE);
        if (c.isTrailingStopEnabled()) {
            addRow(grid, row++, "Trailing Stop", String.format("%.2fx %s",
                    c.getTrailingStopMultiplier(), c.getTrailingStopBasis()), VALUE_NEUTRAL_STYLE);
        }
        if (c.isTargetEnabled()) {
            addRow(grid, row++, "Target", String.format("%.2fx %s",
                    c.getTargetMultiplier(), c.getTargetBasis()), VALUE_NEUTRAL_STYLE);
        }
        addRow(grid, row++, "Slippage", formatPercent(c.getSlippage() * 100), VALUE_NEUTRAL_STYLE);
        addRow(grid, row, "ATR Scaling", c.isAtrScaling() ? "Yes" : "No", VALUE_NEUTRAL_STYLE);
        return buildCard(CONFIG_HEADER, grid);
    }

    // ── Card & Grid builders ─────────────────────────────────────────

    private VBox buildCard(String title, GridPane grid) {
        Label header = new Label(title);
        header.getStyleClass().add(SECTION_HEADER_STYLE);
        VBox card = new VBox(grid);
        card.getStyleClass().add(SECTION_CARD_STYLE);
        card.setPadding(new Insets(CARD_PADDING));
        return new VBox(2, header, card);
    }

    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(GRID_V_GAP);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.NEVER);
        valueCol.setHalignment(Pos.CENTER_RIGHT.getHpos());
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        return grid;
    }

    private void addRow(GridPane grid, int row, String name, String value, String colorStyle) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add(METRIC_LABEL_STYLE);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll(METRIC_VALUE_STYLE, colorStyle);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    // ── Formatting ───────────────────────────────────────────────────

    private static String colorForSign(double value) {
        if (value > 0) return VALUE_POSITIVE_STYLE;
        if (value < 0) return VALUE_NEGATIVE_STYLE;
        return VALUE_NEUTRAL_STYLE;
    }

    private static String colorForThreshold(double value, double threshold) {
        return value >= threshold ? VALUE_POSITIVE_STYLE : VALUE_NEGATIVE_STYLE;
    }

    private static String colorForWinRate(double winRateFraction) {
        return winRateFraction >= 0.5 ? VALUE_POSITIVE_STYLE : VALUE_NEGATIVE_STYLE;
    }

    private static String formatCurrency(double value) {
        double abs = Math.abs(value);
        if (abs >= MILLION) return String.format("%.2fM", value / MILLION);
        if (abs >= THOUSAND) return String.format("%,.0f", value);
        return String.format("%.2f", value);
    }

    private static String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }

    private static String formatRatio(double value) {
        return String.format("%.2f", value);
    }
}
