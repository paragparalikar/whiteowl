package com.whiteowl.workbench.orboptimizer;

import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.rotational.RotationalMetrics;
import com.whiteowl.core.backtest.rotational.optimizer.OptimizableParameter;
import com.whiteowl.core.backtest.rotational.optimizer.OptimizationResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Function;

/**
 * Right-side output pane showing optimization results.
 *
 * <p>Displays:
 * <ol>
 *   <li>Configuration summary used as input</li>
 *   <li>Metric selector (Sharpe, Sortino, Calmar, MaxDD, CAGR)</li>
 *   <li>Line chart for single-parameter optimization</li>
 *   <li>Heatmap for two-parameter optimization</li>
 * </ol>
 */
public final class OptimizationResultPane extends VBox {

    // ── Style constants ──────────────────────────────────────────────
    private static final String PANE_STYLE = "opt-result-pane";
    private static final String HEADER_STYLE = "opt-result-header";
    private static final String CONFIG_SUMMARY_STYLE = "opt-result-config-summary";
    private static final String METRIC_COMBO_STYLE = "opt-result-metric-combo";
    private static final String CHART_CONTAINER_STYLE = "opt-result-chart-container";
    private static final String HEATMAP_CELL_STYLE = "opt-result-heatmap-cell";
    private static final String HEATMAP_LABEL_STYLE = "opt-result-heatmap-label";
    private static final String LEGEND_STYLE = "opt-result-legend";

    private static final int SECTION_SPACING = 8;
    private static final int GRID_H_GAP = 8;
    private static final int GRID_V_GAP = 4;

    private final RotationalBacktestConfig baseConfig;
    private final List<OptimizableParameter> params;
    private final List<OptimizationResult> results;

    private final ComboBox<MetricOption> metricCombo;
    private final VBox chartContainer;

    /** Metric options for the selector. */
    private enum MetricOption {
        SHARPE("Sharpe", RotationalMetrics::getSharpe),
        SORTINO("Sortino", RotationalMetrics::getSortino),
        CALMAR("Calmar", RotationalMetrics::getCalmar),
        MAX_DD("Max Drawdown", RotationalMetrics::getMaxDrawdown),
        CAGR("CAGR", RotationalMetrics::getCagr);

        final String label;
        final Function<RotationalMetrics, Double> extractor;

        MetricOption(String label, Function<RotationalMetrics, Double> extractor) {
            this.label = label;
            this.extractor = extractor;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public OptimizationResultPane(OrbOptimizerPane.OptimizationRunResult runResult) {
        this.baseConfig = runResult.baseConfig();
        this.params = runResult.params();
        this.results = runResult.results();

        this.metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll(MetricOption.values());
        metricCombo.setValue(MetricOption.SHARPE);
        metricCombo.getStyleClass().add(METRIC_COMBO_STYLE);
        metricCombo.setOnAction(e -> refreshChart());

        this.chartContainer = new VBox();
        chartContainer.getStyleClass().add(CHART_CONTAINER_STYLE);
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        getStyleClass().add(PANE_STYLE);
        buildLayout();
        refreshChart();
    }

    private void buildLayout() {
        // Config summary section
        VBox configSection = buildConfigSummary();

        // Metric selector
        Label metricLabel = new Label("Metric:");
        metricLabel.getStyleClass().add(HEADER_STYLE);
        HBox metricRow = new HBox(SECTION_SPACING, metricLabel, metricCombo);
        metricRow.setAlignment(Pos.CENTER_LEFT);
        metricRow.setPadding(new Insets(4, 10, 4, 10));

        // Wrap everything in a scroll pane
        VBox content = new VBox(SECTION_SPACING,
                configSection, new Separator(), metricRow, new Separator(), chartContainer);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private VBox buildConfigSummary() {
        Label header = new Label("Optimization Configuration");
        header.getStyleClass().add(HEADER_STYLE);

        GridPane grid = new GridPane();
        grid.setHgap(GRID_H_GAP);
        grid.setVgap(GRID_V_GAP);
        grid.setPadding(new Insets(4, 10, 4, 10));

        int row = 0;
        row = addSummaryRow(grid, row, "Universe", baseConfig.getUniverseGroupName());
        row = addSummaryRow(grid, row, "OR Minutes", String.valueOf(baseConfig.getOpeningRangeMinutes()));
        row = addSummaryRow(grid, row, "Bar Minutes", String.valueOf(baseConfig.getBarMinutes()));
        row = addSummaryRow(grid, row, "Entry Method", baseConfig.getEntryMethod().name());
        row = addSummaryRow(grid, row, "Ranker", baseConfig.getRankerType().name());
        row = addSummaryRow(grid, row, "Side", baseConfig.getSide().name());
        row = addSummaryRow(grid, row, "Picks", String.valueOf(baseConfig.getPicks()));
        row = addSummaryRow(grid, row, "Stop", String.format("%.1fx %s",
                baseConfig.getStopMultiplier(), baseConfig.getStopBasis()));
        if (baseConfig.isTargetEnabled()) {
            row = addSummaryRow(grid, row, "Target", String.format("%.1fx %s",
                    baseConfig.getTargetMultiplier(), baseConfig.getTargetBasis()));
        }
        row = addSummaryRow(grid, row, "Total Configs", String.valueOf(results.size()));

        // Show optimized parameters
        for (OptimizableParameter p : params) {
            row = addSummaryRow(grid, row, "Optimize: " + p.name(),
                    String.format("%.2f to %.2f (step %.2f)", p.min(), p.max(), p.step()));
        }

        VBox section = new VBox(SECTION_SPACING, header, grid);
        section.getStyleClass().add(CONFIG_SUMMARY_STYLE);
        section.setPadding(new Insets(8, 10, 4, 10));
        return section;
    }

    private int addSummaryRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add(HEADER_STYLE);
        Label val = new Label(value);
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
        return row + 1;
    }

    // ── Chart rendering ──────────────────────────────────────────────

    private void refreshChart() {
        chartContainer.getChildren().clear();
        MetricOption metric = metricCombo.getValue();
        if (metric == null || results.isEmpty()) return;

        if (params.size() == 1) {
            chartContainer.getChildren().add(buildLineChart(metric));
        } else {
            chartContainer.getChildren().add(buildHeatmap(metric));
        }
    }

    private LineChart<String, Number> buildLineChart(MetricOption metric) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(params.get(0).name());
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(metric.label);
        yAxis.setAutoRanging(true);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(metric.label + " vs " + params.get(0).name());
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(400);
        chart.setMinHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (OptimizationResult r : results) {
            if (r.metrics() == null) continue;
            String xLabel = formatParamValue(r.paramValues()[0]);
            double yVal = metric.extractor.apply(r.metrics());
            series.getData().add(new XYChart.Data<>(xLabel, yVal));
        }
        chart.getData().add(series);

        VBox.setVgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private VBox buildHeatmap(MetricOption metric) {
        OptimizableParameter p1 = params.get(0);
        OptimizableParameter p2 = params.get(1);

        int cols = p1.gridSize();
        int rows = p2.gridSize();

        // Build value matrix
        double[][] values = new double[rows][cols];
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        boolean hasData = false;

        for (OptimizationResult r : results) {
            if (r.metrics() == null) continue;
            int col = (int) Math.round((r.paramValues()[0] - p1.min()) / p1.step());
            int row = (int) Math.round((r.paramValues()[1] - p2.min()) / p2.step());
            if (col >= 0 && col < cols && row >= 0 && row < rows) {
                double val = metric.extractor.apply(r.metrics());
                values[row][col] = val;
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
                hasData = true;
            }
        }

        if (!hasData) {
            return new VBox(new Label("No valid results to display"));
        }

        // Title
        Label title = new Label(metric.label + ": " + p1.name() + " vs " + p2.name());
        title.getStyleClass().add(HEADER_STYLE);
        title.setPadding(new Insets(4, 10, 4, 10));

        // Heatmap grid
        GridPane heatmapGrid = new GridPane();
        heatmapGrid.setHgap(1);
        heatmapGrid.setVgap(1);
        heatmapGrid.setPadding(new Insets(4, 10, 4, 10));

        int cellSize = Math.max(20, Math.min(60, 400 / Math.max(cols, rows)));

        // Y-axis label
        Label yLabel = new Label(p2.name());
        yLabel.setRotate(-90);
        yLabel.getStyleClass().add(HEATMAP_LABEL_STYLE);
        heatmapGrid.add(new StackPane(yLabel), 0, 0, 1, rows + 1);

        // X-axis labels (top row)
        for (int c = 0; c < cols; c++) {
            Label xLbl = new Label(formatParamValue(p1.valueAt(c)));
            xLbl.getStyleClass().add(HEATMAP_LABEL_STYLE);
            xLbl.setAlignment(Pos.CENTER);
            heatmapGrid.add(xLbl, c + 1, 0);
        }

        // Y-axis labels (left column) and cells
        for (int r = 0; r < rows; r++) {
            Label yLbl = new Label(formatParamValue(p2.valueAt(r)));
            yLbl.getStyleClass().add(HEATMAP_LABEL_STYLE);
            yLbl.setAlignment(Pos.CENTER_RIGHT);
            heatmapGrid.add(yLbl, 0, r + 1);

            for (int c = 0; c < cols; c++) {
                double val = values[r][c];
                double normalized = (maxVal > minVal) ? (val - minVal) / (maxVal - minVal) : 0.5;
                Color color = interpolateColor(normalized);

                Rectangle rect = new Rectangle(cellSize, cellSize);
                rect.setFill(color);
                rect.setStroke(Color.web("#333333"));
                rect.setStrokeWidth(0.5);

                Text text = new Text(formatMetricValue(val));
                text.setFill(normalized > 0.5 ? Color.BLACK : Color.WHITE);
                text.setStyle("-fx-font-size: 9px;");

                StackPane cell = new StackPane(rect, text);
                heatmapGrid.add(cell, c + 1, r + 1);
            }
        }

        // X-axis title
        Label xTitle = new Label(p1.name());
        xTitle.getStyleClass().add(HEATMAP_LABEL_STYLE);
        xTitle.setPadding(new Insets(4, 0, 0, 0));
        xTitle.setAlignment(Pos.CENTER);

        // Color legend
        HBox legend = buildColorLegend(minVal, maxVal, metric.label);

        VBox heatmapBox = new VBox(SECTION_SPACING, title, heatmapGrid, xTitle, legend);
        heatmapBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(heatmapBox, Priority.ALWAYS);
        return heatmapBox;
    }

    private HBox buildColorLegend(double minVal, double maxVal, String metricName) {
        HBox legend = new HBox(2);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(8, 10, 8, 10));
        legend.getStyleClass().add(LEGEND_STYLE);

        Label minLabel = new Label(formatMetricValue(minVal));
        minLabel.getStyleClass().add(HEATMAP_LABEL_STYLE);

        int gradientSteps = 20;
        for (int i = 0; i < gradientSteps; i++) {
            double normalized = (double) i / (gradientSteps - 1);
            Rectangle rect = new Rectangle(12, 14);
            rect.setFill(interpolateColor(normalized));
            legend.getChildren().add(rect);
        }

        Label maxLabel = new Label(formatMetricValue(maxVal));
        maxLabel.getStyleClass().add(HEATMAP_LABEL_STYLE);

        HBox result = new HBox(4, minLabel, legend, maxLabel);
        result.setAlignment(Pos.CENTER);
        return result;
    }

    /**
     * Interpolate from red (low) through yellow (mid) to green (high).
     */
    private Color interpolateColor(double t) {
        t = Math.max(0, Math.min(1, t));
        if (t < 0.5) {
            // Red to yellow
            double ratio = t * 2;
            return Color.color(0.8, 0.2 + 0.6 * ratio, 0.1);
        } else {
            // Yellow to green
            double ratio = (t - 0.5) * 2;
            return Color.color(0.8 - 0.5 * ratio, 0.8, 0.1 + 0.4 * ratio);
        }
    }

    private static String formatParamValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    private static String formatMetricValue(double value) {
        if (Math.abs(value) < 0.01) return String.format("%.4f", value);
        if (Math.abs(value) < 10) return String.format("%.2f", value);
        return String.format("%.1f", value);
    }
}
