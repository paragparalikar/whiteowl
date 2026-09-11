package com.whiteowl.workbench.driver;

import atlantafx.base.theme.Dracula;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone JavaFX driver that backtests a 30-minute Opening Range Breakout (ORB)
 * strategy on NSE:NIFTY 50 intraday (5-minute) bars and displays results in a window.
 *
 * <p>Run with:
 * <pre>
 * mvn compile exec:java -pl whiteowl-workbench \
 *     -Dexec.mainClass=com.whiteowl.workbench.driver.NiftyOrbDriver
 * </pre>
 */
public class NiftyOrbDriver extends Application {

    // ═══════════════════════════════════════════════════════════════
    //  CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    private static final String SCRIP_ID = "NSE:NIFTY 50";
    private static final Timeframe TIMEFRAME = Timeframe.FIVE_MINUTE;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final int ORB_MINUTES = 30;
    private static final float REWARD_RISK = 1.5f;
    private static final float MIN_RANGE_POINTS = 40f;
    private static final float RISK_PERCENT = 0.02f;
    private static final double INITIAL_CAPITAL = 100_000.0;

    private static final LocalTime ORB_END = LocalTime.of(9, 45);
    private static final LocalTime EXIT_TIME = LocalTime.of(15, 15);

    // ═══════════════════════════════════════════════════════════════
    //  DATA STRUCTURES
    // ═══════════════════════════════════════════════════════════════

    record Trade(LocalDate date, boolean isLong, float entryPrice, float exitPrice,
                 int quantity, double pnl, String exitReason) {
        double pnlPercent() {
            double cost = entryPrice * (double) quantity;
            return cost > 0 ? pnl / cost * 100.0 : 0;
        }
    }

    record DayResult(LocalDate date, double equity, double drawdownPct) {}

    // ═══════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());

        // Ensure standard decorated, resizable window
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(true);

        Bars bars = new FileBarsRepository().load(SCRIP_ID, TIMEFRAME);
        if (bars == null || bars.size() == 0) {
            stage.setTitle("No data for " + SCRIP_ID + " " + TIMEFRAME.getLabel());
            stage.setScene(new Scene(new Label("No data found"), 400, 200));
            stage.show();
            return;
        }

        List<Trade> trades = new ArrayList<>();
        List<DayResult> dayResults = new ArrayList<>();
        runStrategy(bars, trades, dayResults);

        VBox metricsContent = buildMetricsPanel(trades, dayResults);
        ScrollPane metricsScroll = new ScrollPane(metricsContent);
        metricsScroll.setFitToWidth(true);
        metricsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        metricsScroll.setMinWidth(300);
        metricsScroll.setMaxWidth(320);

        VBox charts = buildChartsPanel(dayResults);
        HBox root = new HBox(15, metricsScroll, charts);
        root.setPadding(new Insets(15));
        HBox.setHgrow(charts, Priority.ALWAYS);

        stage.setTitle("Nifty 50 ORB Backtest — " + ORB_MINUTES + "min, " + REWARD_RISK + "R, "
                + bars.size() + " bars");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(new Scene(root, 1280, 720));
        stage.show();
    }

    // ═══════════════════════════════════════════════════════════════
    //  STRATEGY ENGINE
    // ═══════════════════════════════════════════════════════════════

    private void runStrategy(Bars bars, List<Trade> trades, List<DayResult> dayResults) {
        int size = bars.size();
        double equity = INITIAL_CAPITAL;
        double peakEquity = equity;

        int dayStart = 0;
        LocalDate currentDate = toDate(bars.getTimestamp(0));

        for (int i = 1; i <= size; i++) {
            LocalDate barDate = (i < size) ? toDate(bars.getTimestamp(i)) : null;
            if (barDate != null && barDate.equals(currentDate)) continue;

            // End of day — process dayStart..i
            Trade trade = processDay(bars, dayStart, i, equity, currentDate);
            if (trade != null) {
                equity += trade.pnl;
                trades.add(trade);
            }
            peakEquity = Math.max(peakEquity, equity);
            double dd = peakEquity > 0 ? (equity - peakEquity) / peakEquity * 100.0 : 0;
            dayResults.add(new DayResult(currentDate, equity, dd));

            dayStart = i;
            currentDate = barDate;
        }
    }

    private Trade processDay(Bars bars, int start, int end, double equity, LocalDate date) {
        int dayBars = end - start;
        if (dayBars < 8) return null;

        // ── Build opening range ──────────────────────────────────
        float orbHigh = Float.NEGATIVE_INFINITY;
        float orbLow = Float.POSITIVE_INFINITY;
        int orbEndIdx = start;
        float twapSum = 0;
        int twapCount = 0;

        for (int i = start; i < end; i++) {
            LocalTime time = toTime(bars.getTimestamp(i));
            if (time.isBefore(ORB_END)) {
                orbHigh = Math.max(orbHigh, bars.getHigh(i));
                orbLow = Math.min(orbLow, bars.getLow(i));
                float tp = (bars.getHigh(i) + bars.getLow(i) + bars.getClose(i)) / 3f;
                twapSum += tp;
                twapCount++;
                orbEndIdx = i + 1;
            } else {
                break;
            }
        }

        float range = orbHigh - orbLow;
        if (range < MIN_RANGE_POINTS || orbEndIdx >= end) return null;

        // ── Scan for entry after ORB ─────────────────────────────
        boolean inTrade = false;
        boolean isLong = false;
        float entryPrice = 0;
        float stopLoss = 0;
        float target = 0;
        int qty = 0;

        for (int i = orbEndIdx; i < end; i++) {
            LocalTime time = toTime(bars.getTimestamp(i));
            float h = bars.getHigh(i);
            float l = bars.getLow(i);
            float c = bars.getClose(i);

            // Update session TWAP
            twapSum += (h + l + c) / 3f;
            twapCount++;
            float twap = twapSum / twapCount;

            // ── Forced exit ──────────────────────────────────────
            if (!time.isBefore(EXIT_TIME)) {
                if (inTrade) {
                    double pnl = isLong ? (c - entryPrice) * qty : (entryPrice - c) * qty;
                    return new Trade(date, isLong, entryPrice, c, qty, pnl, "TIME");
                }
                return null; // no trade today
            }

            // ── Manage open position ─────────────────────────────
            if (inTrade) {
                if (isLong) {
                    if (l <= stopLoss) {
                        double pnl = (double)(stopLoss - entryPrice) * qty;
                        return new Trade(date, true, entryPrice, stopLoss, qty, pnl, "STOP");
                    }
                    if (h >= target) {
                        double pnl = (double)(target - entryPrice) * qty;
                        return new Trade(date, true, entryPrice, target, qty, pnl, "TARGET");
                    }
                } else {
                    if (h >= stopLoss) {
                        double pnl = (double)(entryPrice - stopLoss) * qty;
                        return new Trade(date, false, entryPrice, stopLoss, qty, pnl, "STOP");
                    }
                    if (l <= target) {
                        double pnl = (double)(entryPrice - target) * qty;
                        return new Trade(date, false, entryPrice, target, qty, pnl, "TARGET");
                    }
                }
                continue;
            }

            // ── Entry logic ──────────────────────────────────────
            if (i + 1 >= end) continue;
            double riskAmt = equity * RISK_PERCENT;
            int posQty = Math.max(1, (int) (riskAmt / range));

            if (c > orbHigh && c > twap) {
                entryPrice = bars.getOpen(i + 1);
                stopLoss = orbLow;
                target = entryPrice + range * REWARD_RISK;
                qty = posQty;
                isLong = true;
                inTrade = true;
                i++; // advance to entry bar
            } else if (c < orbLow && c < twap) {
                entryPrice = bars.getOpen(i + 1);
                stopLoss = orbHigh;
                target = entryPrice - range * REWARD_RISK;
                qty = posQty;
                isLong = false;
                inTrade = true;
                i++; // advance to entry bar
            }
        }

        // Still in trade at end of data
        if (inTrade) {
            float exitPrice = bars.getClose(end - 1);
            double pnl = isLong ? (exitPrice - entryPrice) * qty : (entryPrice - exitPrice) * qty;
            return new Trade(date, isLong, entryPrice, exitPrice, qty, pnl, "EOD");
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  METRICS PANEL
    // ═══════════════════════════════════════════════════════════════

    private VBox buildMetricsPanel(List<Trade> trades, List<DayResult> dayResults) {
        double finalEquity = dayResults.isEmpty() ? INITIAL_CAPITAL
                : dayResults.getLast().equity;
        double netProfit = finalEquity - INITIAL_CAPITAL;
        double netProfitPct = netProfit / INITIAL_CAPITAL * 100.0;

        // CAGR
        long tradingDays = dayResults.size();
        double years = tradingDays / 252.0;
        double cagr = years > 0 ? (Math.pow(finalEquity / INITIAL_CAPITAL, 1.0 / years) - 1) * 100 : 0;

        // Max drawdown
        double maxDD = dayResults.stream().mapToDouble(DayResult::drawdownPct).min().orElse(0);

        // Sharpe (annualized from daily equity returns)
        double sharpe = computeSharpe(dayResults);

        // Trade stats
        int total = trades.size();
        long winners = trades.stream().filter(t -> t.pnl > 0).count();
        long losers = trades.stream().filter(t -> t.pnl <= 0).count();
        double winRate = total > 0 ? (double) winners / total * 100 : 0;

        double avgWin = trades.stream().filter(t -> t.pnl > 0)
                .mapToDouble(Trade::pnlPercent).average().orElse(0);
        double avgLoss = trades.stream().filter(t -> t.pnl <= 0)
                .mapToDouble(Trade::pnlPercent).average().orElse(0);
        double largestWin = trades.stream().mapToDouble(t -> t.pnl).max().orElse(0);
        double largestLoss = trades.stream().mapToDouble(t -> t.pnl).min().orElse(0);

        double grossProfit = trades.stream().filter(t -> t.pnl > 0).mapToDouble(t -> t.pnl).sum();
        double grossLoss = Math.abs(trades.stream().filter(t -> t.pnl <= 0).mapToDouble(t -> t.pnl).sum());
        double profitFactor = grossLoss > 0 ? grossProfit / grossLoss : Double.POSITIVE_INFINITY;
        double expectancy = total > 0 ? netProfit / total : 0;

        long targetExits = trades.stream().filter(t -> "TARGET".equals(t.exitReason)).count();
        long stopExits = trades.stream().filter(t -> "STOP".equals(t.exitReason)).count();
        long timeExits = trades.stream().filter(t -> "TIME".equals(t.exitReason)
                || "EOD".equals(t.exitReason)).count();
        long longTrades = trades.stream().filter(Trade::isLong).count();
        long shortTrades = total - longTrades;

        // Date range
        String dateRange = "";
        if (!dayResults.isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yy");
            dateRange = dayResults.getFirst().date.format(fmt)
                    + " to " + dayResults.getLast().date.format(fmt);
        }

        // Build labels
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(10));
        panel.setMinWidth(280);
        panel.setMaxWidth(300);
        panel.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");

        panel.getChildren().addAll(
                sectionTitle("Nifty 50 ORB Backtest"),
                metricLabel("Period", dateRange),
                metricLabel("Trading Days", String.valueOf(tradingDays)),
                new Separator(),
                sectionTitle("Performance"),
                metricLabel("Net Profit", String.format("%,.0f", netProfit),
                        netProfit >= 0 ? Color.LIMEGREEN : Color.TOMATO),
                metricLabel("Net Profit %", String.format("%.2f%%", netProfitPct),
                        netProfitPct >= 0 ? Color.LIMEGREEN : Color.TOMATO),
                metricLabel("CAGR", String.format("%.2f%%", cagr)),
                metricLabel("Initial Capital", String.format("%,.0f", INITIAL_CAPITAL)),
                metricLabel("Final Capital", String.format("%,.0f", finalEquity)),
                new Separator(),
                sectionTitle("Risk"),
                metricLabel("Max Drawdown", String.format("%.2f%%", maxDD), Color.TOMATO),
                metricLabel("Sharpe Ratio", String.format("%.2f", sharpe)),
                new Separator(),
                sectionTitle("Trades"),
                metricLabel("Total Trades", String.valueOf(total)),
                metricLabel("Win Rate", String.format("%.1f%%", winRate)),
                metricLabel("Winners / Losers", winners + " / " + losers),
                metricLabel("Long / Short", longTrades + " / " + shortTrades),
                metricLabel("Avg Win", String.format("%.2f%%", avgWin)),
                metricLabel("Avg Loss", String.format("%.2f%%", avgLoss)),
                metricLabel("Largest Win", String.format("%,.0f", largestWin)),
                metricLabel("Largest Loss", String.format("%,.0f", largestLoss)),
                metricLabel("Profit Factor", String.format("%.2f", profitFactor)),
                metricLabel("Expectancy", String.format("%,.0f / trade", expectancy)),
                new Separator(),
                sectionTitle("Exit Breakdown"),
                metricLabel("Target", String.valueOf(targetExits)),
                metricLabel("Stop", String.valueOf(stopExits)),
                metricLabel("Time / EOD", String.valueOf(timeExits))
        );
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CHARTS PANEL
    // ═══════════════════════════════════════════════════════════════

    private VBox buildChartsPanel(List<DayResult> dayResults) {
        LineChart<Number, Number> equityChart = buildEquityChart(dayResults);
        LineChart<Number, Number> ddChart = buildDrawdownChart(dayResults);

        VBox.setVgrow(equityChart, Priority.ALWAYS);
        VBox.setVgrow(ddChart, Priority.ALWAYS);

        VBox box = new VBox(10, equityChart, ddChart);
        return box;
    }

    private LineChart<Number, Number> buildEquityChart(List<DayResult> dayResults) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Trading Day");
        xAxis.setTickLabelFormatter(dayIndexFormatter(dayResults));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Equity");
        yAxis.setForceZeroInRange(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Equity Curve");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Equity");
        for (int i = 0; i < dayResults.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, dayResults.get(i).equity));
        }
        chart.getData().add(series);

        // Style the line green
        chart.lookup(".chart-series-line").setStyle(
                "-fx-stroke: #50fa7b; -fx-stroke-width: 1.5px;");
        return chart;
    }

    private LineChart<Number, Number> buildDrawdownChart(List<DayResult> dayResults) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Trading Day");
        xAxis.setTickLabelFormatter(dayIndexFormatter(dayResults));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Drawdown %");
        yAxis.setForceZeroInRange(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Drawdown");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Drawdown");
        for (int i = 0; i < dayResults.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, dayResults.get(i).drawdownPct));
        }
        chart.getData().add(series);

        chart.lookup(".chart-series-line").setStyle(
                "-fx-stroke: #ff5555; -fx-stroke-width: 1.5px;");
        return chart;
    }

    private NumberAxis.DefaultFormatter dayIndexFormatter(List<DayResult> dayResults) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");
        return new NumberAxis.DefaultFormatter(new NumberAxis()) {
            @Override
            public String toString(Number value) {
                int idx = value.intValue();
                if (idx >= 0 && idx < dayResults.size()) {
                    return dayResults.get(idx).date.format(fmt);
                }
                return "";
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private double computeSharpe(List<DayResult> dayResults) {
        if (dayResults.size() < 2) return 0;
        double[] returns = new double[dayResults.size() - 1];
        for (int i = 1; i < dayResults.size(); i++) {
            double prev = dayResults.get(i - 1).equity;
            returns[i - 1] = prev > 0 ? (dayResults.get(i).equity - prev) / prev : 0;
        }
        double mean = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;
        double variance = 0;
        for (double r : returns) variance += (r - mean) * (r - mean);
        variance /= returns.length;
        double stddev = Math.sqrt(variance);
        return stddev > 0 ? (mean / stddev) * Math.sqrt(252) : 0;
    }

    private static LocalDate toDate(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalDate();
    }

    private static LocalTime toTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalTime();
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setPadding(new Insets(6, 0, 2, 0));
        return label;
    }

    private HBox metricLabel(String name, String value) {
        return metricLabel(name, value, null);
    }

    private HBox metricLabel(String name, String value, Color valueColor) {
        Label nameLabel = new Label(name);
        nameLabel.setMinWidth(140);
        nameLabel.setStyle("-fx-text-fill: -color-fg-muted;");
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        if (valueColor != null) {
            valueLabel.setTextFill(valueColor);
        }
        HBox box = new HBox(nameLabel, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
