package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.EquityCurve;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class EquityCurvePane extends VBox {

    private static final String PANE_STYLE = "backtest-equity-curve-pane";
    private static final Color EQUITY_COLOR = Color.web("#4ec9b0");
    private static final Color DRAWDOWN_COLOR = Color.web("#ef5350", 0.3);
    private static final Color DRAWDOWN_STROKE = Color.web("#ef5350", 0.6);
    private static final Color CANVAS_BG = Color.web("#1e1f22");
    private static final Color GRID_LINE = Color.web("#2b2d30");
    private static final Color AXIS_TEXT_COLOR = Color.web("#868a91");
    private static final Color ZERO_LINE_COLOR = Color.web("#3c3f41");
    private static final double LINE_WIDTH = 1.2;
    private static final double AXIS_FONT_SIZE = 9;
    private static final double PADDING_H = 40;
    private static final double PADDING_V = 12;
    private static final double EQUITY_RATIO = 0.65;
    private static final double GAP = 8;
    private static final int GRID_LINES = 4;
    private static final int CONTENT_PADDING = 10;
    private static final double BILLION = 1_000_000_000;
    private static final double MILLION = 1_000_000;
    private static final double THOUSAND = 1_000;

    private final Canvas canvas;
    private BacktestReport report;

    public EquityCurvePane() {
        this.canvas = new Canvas();
        getStyleClass().add(PANE_STYLE);
        setPadding(new Insets(CONTENT_PADDING));
        getChildren().add(canvas);
        canvas.widthProperty().addListener((obs, o, n) -> render());
        canvas.heightProperty().addListener((obs, o, n) -> render());
        widthProperty().addListener((obs, o, n) -> resizeCanvas());
        heightProperty().addListener((obs, o, n) -> resizeCanvas());
    }

    public void setReport(BacktestReport report) {
        this.report = report;
        resizeCanvas();
    }

    private void resizeCanvas() {
        double w = getWidth() - CONTENT_PADDING * 2;
        double h = getHeight() - CONTENT_PADDING * 2;
        if (w > 0 && h > 0) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(CANVAS_BG);
        gc.fillRect(0, 0, w, h);
        if (report == null) return;
        EquityCurve curve = report.getEquityCurve();
        float[] dd = report.getDrawdownCurve();
        if (curve == null || curve.getSize() < 2) return;
        if (dd == null || dd.length < 2) return;
        double equityH = (h - GAP) * EQUITY_RATIO;
        double ddH = h - equityH - GAP;
        renderEquity(gc, w, equityH, curve);
        renderDrawdown(gc, w, equityH + GAP, ddH, dd);
    }

    private void renderEquity(GraphicsContext gc, double w, double h, EquityCurve curve) {
        float[] values = curve.getValues();
        int size = curve.getSize();
        float min = findMin(values, size);
        float max = findMax(values, size);
        float range = max - min;
        if (range <= 0) range = 1;
        drawGrid(gc, 0, w, h, min, max, range, true);
        double chartW = w - PADDING_H * 2;
        double chartH = h - PADDING_V * 2;
        gc.setStroke(EQUITY_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        gc.beginPath();
        for (int i = 0; i < size; i++) {
            double x = PADDING_H + (double) i / (size - 1) * chartW;
            double y = PADDING_V + chartH * (1 - (values[i] - min) / range);
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private void renderDrawdown(GraphicsContext gc, double w, double top, double h, float[] dd) {
        int size = dd.length;
        float min = findMin(dd, size);
        float max = 0;
        float range = max - min;
        if (range <= 0) range = 1;
        drawGrid(gc, top, w, h, min, max, range, false);
        double chartW = w - PADDING_H * 2;
        double chartH = h - PADDING_V * 2;
        double zeroY = top + PADDING_V;
        gc.setStroke(ZERO_LINE_COLOR);
        gc.setLineWidth(0.5);
        gc.strokeLine(PADDING_H, zeroY, w - PADDING_H, zeroY);
        gc.setFill(DRAWDOWN_COLOR);
        gc.beginPath();
        gc.moveTo(PADDING_H, zeroY);
        for (int i = 0; i < size; i++) {
            double x = PADDING_H + (double) i / (size - 1) * chartW;
            double y = top + PADDING_V + chartH * (1 - (dd[i] - min) / range);
            gc.lineTo(x, y);
        }
        gc.lineTo(PADDING_H + chartW, zeroY);
        gc.closePath();
        gc.fill();
        gc.setStroke(DRAWDOWN_STROKE);
        gc.setLineWidth(LINE_WIDTH);
        gc.beginPath();
        for (int i = 0; i < size; i++) {
            double x = PADDING_H + (double) i / (size - 1) * chartW;
            double y = top + PADDING_V + chartH * (1 - (dd[i] - min) / range);
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private void drawGrid(GraphicsContext gc, double top, double w, double h,
                           float min, float max, float range, boolean showLabels) {
        gc.setStroke(GRID_LINE);
        gc.setLineWidth(0.5);
        gc.setFill(AXIS_TEXT_COLOR);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        double chartH = h - PADDING_V * 2;
        for (int i = 0; i <= GRID_LINES; i++) {
            double ratio = (double) i / GRID_LINES;
            double y = top + PADDING_V + chartH * ratio;
            gc.strokeLine(PADDING_H, y, w - PADDING_H, y);
            if (showLabels) {
                float value = (float) (max - ratio * range);
                gc.fillText(formatAxisValue(value), 2, y + 3);
            } else {
                float value = (float) (max - ratio * range);
                gc.fillText(formatPercent(value), 2, y + 3);
            }
        }
    }

    private static float findMin(float[] values, int size) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            if (values[i] < min) min = values[i];
        }
        return min;
    }

    private static float findMax(float[] values, int size) {
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            if (values[i] > max) max = values[i];
        }
        return max;
    }

    private static String formatAxisValue(float value) {
        double abs = Math.abs(value);
        if (abs >= MILLION) return String.format("%.1fM", value / MILLION);
        if (abs >= THOUSAND) return String.format("%.0fK", value / THOUSAND);
        return String.format("%.0f", value);
    }

    private static String formatPercent(float value) {
        return String.format("%.1f%%", value);
    }

}
