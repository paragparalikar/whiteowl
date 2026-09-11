package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class ReportPane extends ScrollPane {

    private static final String SCROLL_STYLE = "backtest-report-scroll";
    private static final String CONTENT_STYLE = "backtest-report-content";
    private static final String SECTION_CARD_STYLE = "backtest-report-card";
    private static final String SECTION_HEADER_STYLE = "backtest-report-section-header";
    private static final String METRIC_LABEL_STYLE = "backtest-report-metric-label";
    private static final String METRIC_VALUE_STYLE = "backtest-report-metric-value";
    private static final String VALUE_POSITIVE_STYLE = "backtest-report-value-positive";
    private static final String VALUE_NEGATIVE_STYLE = "backtest-report-value-negative";
    private static final String VALUE_NEUTRAL_STYLE = "backtest-report-value-neutral";
    private static final String PERFORMANCE_HEADER = "Performance";
    private static final String RISK_HEADER = "Risk";
    private static final String TRADES_HEADER = "Trades";
    private static final String STREAKS_HEADER = "Streaks";
    private static final String CONFIG_HEADER = "Configuration";
    private static final String INPUTS_HEADER = "Strategy Inputs";
    private static final String CAPITAL_FORMAT = "%.0f";
    private static final String PERCENT_FORMAT = "%.2f%%";
    private static final String NO_REPORT_TEXT = "Run a backtest to see results";
    private static final String DAYS_FORMAT = "%.0f days";
    private static final String DAYS_DECIMAL_FORMAT = "%.1f days";
    private static final int GRID_V_GAP = 1;
    private static final int CONTENT_SPACING = 8;
    private static final int CONTENT_PADDING = 10;
    private static final int CARD_PADDING = 8;
    private static final double BILLION = 1_000_000_000;
    private static final double MILLION = 1_000_000;
    private static final double THOUSAND = 1_000;

    private final VBox content;

    public ReportPane() {
        this.content = new VBox(CONTENT_SPACING);
        getStyleClass().add(SCROLL_STYLE);
        content.getStyleClass().add(CONTENT_STYLE);
        content.setPadding(new Insets(CONTENT_PADDING));
        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        showEmptyState();
    }

    public void setReport(BacktestReport report, BacktestConfig config) {
        content.getChildren().clear();
        if (report == null || report.getTotalTrades() == 0) {
            showEmptyState();
            return;
        }
        content.getChildren().addAll(
                buildPerformanceCard(report),
                buildRiskCard(report),
                buildTradesCard(report),
                buildStreaksCard(report),
                buildConfigCard(config)
        );
        Map<String, Number> inputs = config.getStrategyInputs();
        if (inputs != null && !inputs.isEmpty()) {
            content.getChildren().add(buildInputsCard(inputs));
        }
    }

    private void showEmptyState() {
        Label emptyLabel = new Label(NO_REPORT_TEXT);
        emptyLabel.getStyleClass().add(METRIC_LABEL_STYLE);
        content.getChildren().add(emptyLabel);
    }

    private VBox buildPerformanceCard(BacktestReport report) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Net Profit", formatCurrency(report.getNetProfit()), colorForSign(report.getNetProfit()));
        addRow(grid, row++, "Net Profit %", formatPercent(report.getNetProfitPercent()), colorForSign(report.getNetProfitPercent()));
        addRow(grid, row++, "CAGR", formatPercent(report.getCagr()), colorForSign(report.getCagr()));
        addRow(grid, row++, "Initial Capital", formatCurrency(report.getInitialCapital()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row, "Final Capital", formatCurrency(report.getFinalCapital()), VALUE_NEUTRAL_STYLE);
        return buildCard(PERFORMANCE_HEADER, grid);
    }

    private VBox buildRiskCard(BacktestReport report) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Max Drawdown", formatPercent(report.getMaxDrawdown()), VALUE_NEGATIVE_STYLE);
        addRow(grid, row++, "Max DD Duration", String.format(DAYS_FORMAT, report.getMaxDrawdownDurationDays()), VALUE_NEGATIVE_STYLE);
        addRow(grid, row++, "Sharpe Ratio", formatRatio(report.getSharpeRatio()), colorForSign(report.getSharpeRatio()));
        addRow(grid, row++, "Sortino Ratio", formatRatio(report.getSortinoRatio()), colorForSign(report.getSortinoRatio()));
        addRow(grid, row, "Calmar Ratio", formatRatio(report.getCalmarRatio()), colorForSign(report.getCalmarRatio()));
        return buildCard(RISK_HEADER, grid);
    }

    private VBox buildTradesCard(BacktestReport report) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Total Trades", String.valueOf(report.getTotalTrades()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Win Rate", formatPercent(report.getWinRate()), colorForWinRate(report.getWinRate()));
        addRow(grid, row++, "Avg Win", formatPercent(report.getAverageWin()), VALUE_POSITIVE_STYLE);
        addRow(grid, row++, "Avg Loss", formatPercent(report.getAverageLoss()), VALUE_NEGATIVE_STYLE);
        addRow(grid, row++, "Largest Win", formatPercent(report.getLargestWin()), VALUE_POSITIVE_STYLE);
        addRow(grid, row++, "Largest Loss", formatPercent(report.getLargestLoss()), VALUE_NEGATIVE_STYLE);
        addRow(grid, row++, "Profit Factor", formatRatio(report.getProfitFactor()), colorForThreshold(report.getProfitFactor(), 1f));
        addRow(grid, row++, "Payoff Ratio", formatRatio(report.getPayoffRatio()), colorForThreshold(report.getPayoffRatio(), 1f));
        addRow(grid, row, "Expectancy", formatPercent(report.getExpectancy()), colorForSign(report.getExpectancy()));
        return buildCard(TRADES_HEADER, grid);
    }

    private VBox buildStreaksCard(BacktestReport report) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Max Consec. Wins", String.valueOf(report.getMaxConsecutiveWins()), VALUE_POSITIVE_STYLE);
        addRow(grid, row++, "Max Consec. Losses", String.valueOf(report.getMaxConsecutiveLosses()), VALUE_NEGATIVE_STYLE);
        addRow(grid, row, "Avg Trade Duration", String.format(DAYS_DECIMAL_FORMAT, report.getAverageTradeDurationDays()), VALUE_NEUTRAL_STYLE);
        return buildCard(STREAKS_HEADER, grid);
    }

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

    private static String colorForSign(float value) {
        if (value > 0) return VALUE_POSITIVE_STYLE;
        if (value < 0) return VALUE_NEGATIVE_STYLE;
        return VALUE_NEUTRAL_STYLE;
    }

    private static String colorForThreshold(float value, float threshold) {
        if (value >= threshold) return VALUE_POSITIVE_STYLE;
        return VALUE_NEGATIVE_STYLE;
    }

    private static String colorForWinRate(float winRate) {
        if (winRate >= 50f) return VALUE_POSITIVE_STYLE;
        return VALUE_NEGATIVE_STYLE;
    }

    private static String formatCurrency(float value) {
        double abs = Math.abs(value);
        if (abs >= BILLION) return String.format("%.2fB", value / BILLION);
        if (abs >= MILLION) return String.format("%.2fM", value / MILLION);
        if (abs >= THOUSAND) return String.format("%,.0f", value);
        return String.format("%.2f", value);
    }

    private static String formatPercent(float value) {
        return String.format("%.2f%%", value);
    }

    private static String formatRatio(float value) {
        return String.format("%.2f", value);
    }

    private VBox buildConfigCard(BacktestConfig config) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        addRow(grid, row++, "Scrip Type", config.getScripType().getDisplayLabel(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Timeframe", config.getTimeframe().getDisplayLabel(), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Offset", String.valueOf(config.getOffset()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Initial Capital", String.format(CAPITAL_FORMAT, config.getInitialCapital()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Cost %", String.format(PERCENT_FORMAT, config.getCostPercent()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row++, "Slippage %", String.format(PERCENT_FORMAT, config.getSlippagePercent()), VALUE_NEUTRAL_STYLE);
        addRow(grid, row, "Volume Participation %", String.format(PERCENT_FORMAT, config.getVolumeParticipationPercent()), VALUE_NEUTRAL_STYLE);
        return buildCard(CONFIG_HEADER, grid);
    }

    private VBox buildInputsCard(Map<String, Number> inputs) {
        GridPane grid = createMetricsGrid();
        int row = 0;
        for (Map.Entry<String, Number> entry : inputs.entrySet()) {
            addRow(grid, row++, entry.getKey(), formatInputValue(entry.getValue()), VALUE_NEUTRAL_STYLE);
        }
        return buildCard(INPUTS_HEADER, grid);
    }

    private static String formatInputValue(Number value) {
        if (value instanceof Integer) return String.valueOf(value.intValue());
        if (value instanceof Long) return String.valueOf(value.longValue());
        double d = value.doubleValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

}
