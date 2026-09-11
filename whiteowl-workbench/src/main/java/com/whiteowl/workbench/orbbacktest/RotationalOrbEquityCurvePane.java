package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.RotationalBacktestReport;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Canvas-based equity curve chart for the rotational ORB backtest.
 * Shows combined, long-only, and short-only equity curves starting at zero
 * (initial capital subtracted so the Y-axis represents P&amp;L from start).
 */
public final class RotationalOrbEquityCurvePane extends VBox {

    private static final String PANE_STYLE = "rot-orb-equity-pane";

    // Colors
    private static final Color EQUITY_COLOR = Color.web("#4ec9b0");
    private static final Color LONG_EQUITY_COLOR = Color.web("#64b5f6");
    private static final Color SHORT_EQUITY_COLOR = Color.web("#ffb74d");
    private static final Color CANVAS_BG = Color.web("#1e1f22");
    private static final Color GRID_LINE = Color.web("#2b2d30");
    private static final Color AXIS_TEXT = Color.web("#868a91");
    private static final Color ZERO_LINE = Color.web("#3c3f41");
    private static final Color LEGEND_TEXT = Color.web("#bbbbbb");

    // Layout
    private static final double LINE_WIDTH = 1.2;
    private static final double THIN_LINE = 0.8;
    private static final double AXIS_FONT_SIZE = 9;
    private static final double LEGEND_FONT_SIZE = 10;
    private static final double PADDING_H = 50;
    private static final double PADDING_V = 12;
    private static final int GRID_LINES = 4;
    private static final int CONTENT_PADDING = 10;
    private static final double MILLION = 1_000_000;
    private static final double THOUSAND = 1_000;
    private static final int DATE_LABELS = 6;
    private static final double LEGEND_HEIGHT = 20;
    private static final double DATE_AXIS_HEIGHT = 18;
    private static final double LEGEND_SWATCH = 12;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Canvas canvas;
    private RotationalBacktestReport report;

    public RotationalOrbEquityCurvePane() {
        this.canvas = new Canvas();
        getStyleClass().add(PANE_STYLE);
        setPadding(new Insets(CONTENT_PADDING));
        getChildren().add(canvas);
        canvas.widthProperty().addListener((obs, o, n) -> render());
        canvas.heightProperty().addListener((obs, o, n) -> render());
        widthProperty().addListener((obs, o, n) -> resizeCanvas());
        heightProperty().addListener((obs, o, n) -> resizeCanvas());
    }

    public void setReport(RotationalBacktestReport report) {
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

        double[] equity = report.getEquityCurve();
        long[] dates = report.getDates();
        if (equity == null || equity.length < 2) return;

        double legendH = LEGEND_HEIGHT;
        double equityH = h - legendH - DATE_AXIS_HEIGHT;

        renderLegend(gc, w, 0);
        renderEquity(gc, w, legendH, equityH, equity, dates);
    }

    // ── Legend ────────────────────────────────────────────────────────

    private void renderLegend(GraphicsContext gc, double w, double top) {
        gc.setFont(Font.font(LEGEND_FONT_SIZE));
        double x = PADDING_H;

        // Combined
        gc.setFill(EQUITY_COLOR);
        gc.fillRect(x, top + 4, LEGEND_SWATCH, LEGEND_SWATCH);
        gc.setFill(LEGEND_TEXT);
        gc.fillText("Combined", x + LEGEND_SWATCH + 4, top + 14);
        x += 80;

        // Long
        gc.setFill(LONG_EQUITY_COLOR);
        gc.fillRect(x, top + 4, LEGEND_SWATCH, LEGEND_SWATCH);
        gc.setFill(LEGEND_TEXT);
        gc.fillText("Long", x + LEGEND_SWATCH + 4, top + 14);
        x += 60;

        // Short
        gc.setFill(SHORT_EQUITY_COLOR);
        gc.fillRect(x, top + 4, LEGEND_SWATCH, LEGEND_SWATCH);
        gc.setFill(LEGEND_TEXT);
        gc.fillText("Short", x + LEGEND_SWATCH + 4, top + 14);
    }

    // ── Equity ───────────────────────────────────────────────────────

    private void renderEquity(GraphicsContext gc, double w, double top, double h,
                              double[] equity, long[] dates) {
        int size = equity.length;
        double[] longEq = report.getLongEquityCurve();
        double[] shortEq = report.getShortEquityCurve();
        // Shift each curve by its own starting value so all begin at 0
        double[] eqShifted = shiftCurve(equity, equity[0]);
        double[] longShifted = longEq != null ? shiftCurve(longEq, longEq[0]) : null;
        double[] shortShifted = shortEq != null ? shiftCurve(shortEq, shortEq[0]) : null;

        // Find global min/max across all three shifted curves
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            min = Math.min(min, eqShifted[i]);
            max = Math.max(max, eqShifted[i]);
            if (longShifted != null && i < longShifted.length) {
                min = Math.min(min, longShifted[i]);
                max = Math.max(max, longShifted[i]);
            }
            if (shortShifted != null && i < shortShifted.length) {
                min = Math.min(min, shortShifted[i]);
                max = Math.max(max, shortShifted[i]);
            }
        }
        double range = max - min;
        if (range <= 0) range = 1;

        drawGrid(gc, top, w, h, min, max, range, true);
        drawDateAxis(gc, top + h, w, dates);

        // Draw zero line
        double chartH = h - PADDING_V * 2;
        double zeroY = top + PADDING_V + chartH * (1 - (0 - min) / range);
        gc.setStroke(ZERO_LINE);
        gc.setLineWidth(0.5);
        gc.strokeLine(PADDING_H, zeroY, w - PADDING_H, zeroY);

        double chartW = w - PADDING_H * 2;

        // Long equity (blue, thin)
        if (longShifted != null && longShifted.length >= 2) {
            drawLine(gc, longShifted, size, top, chartW, chartH, min, range, LONG_EQUITY_COLOR, THIN_LINE);
        }

        // Short equity (orange, thin)
        if (shortShifted != null && shortShifted.length >= 2) {
            drawLine(gc, shortShifted, size, top, chartW, chartH, min, range, SHORT_EQUITY_COLOR, THIN_LINE);
        }

        // Combined equity (green, normal)
        drawLine(gc, eqShifted, size, top, chartW, chartH, min, range, EQUITY_COLOR, LINE_WIDTH);
    }

    private static double[] shiftCurve(double[] curve, double offset) {
        double[] shifted = new double[curve.length];
        for (int i = 0; i < curve.length; i++) {
            shifted[i] = curve[i] - offset;
        }
        return shifted;
    }

    private void drawLine(GraphicsContext gc, double[] values, int size, double top,
                          double chartW, double chartH, double min, double range,
                          Color color, double lineWidth) {
        int len = Math.min(values.length, size);
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.beginPath();
        for (int i = 0; i < len; i++) {
            double x = PADDING_H + (double) i / (len - 1) * chartW;
            double y = top + PADDING_V + chartH * (1 - (values[i] - min) / range);
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    // ── Grid & Axes ──────────────────────────────────────────────────

    private void drawGrid(GraphicsContext gc, double top, double w, double h,
                          double min, double max, double range, boolean isCurrency) {
        gc.setStroke(GRID_LINE);
        gc.setLineWidth(0.5);
        gc.setFill(AXIS_TEXT);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        double chartH = h - PADDING_V * 2;
        for (int i = 0; i <= GRID_LINES; i++) {
            double ratio = (double) i / GRID_LINES;
            double y = top + PADDING_V + chartH * ratio;
            gc.strokeLine(PADDING_H, y, w - PADDING_H, y);
            double value = max - ratio * range;
            gc.fillText(isCurrency ? formatAxisValue(value) : formatPercent(value), 2, y + 3);
        }
    }

    private void drawDateAxis(GraphicsContext gc, double y, double w, long[] dates) {
        if (dates.length < 2) return;
        gc.setFill(AXIS_TEXT);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        double chartW = w - PADDING_H * 2;
        int count = Math.min(DATE_LABELS, dates.length);
        for (int i = 0; i < count; i++) {
            int idx = (int) ((long) i * (dates.length - 1) / (count - 1));
            double x = PADDING_H + (double) idx / (dates.length - 1) * chartW;
            String label = Instant.ofEpochMilli(dates[idx]).atZone(IST).toLocalDate().format(DATE_FMT);
            gc.fillText(label, x - 25, y + 12);
        }
    }

    private static String formatAxisValue(double value) {
        double abs = Math.abs(value);
        if (abs >= MILLION) return String.format("%.1fM", value / MILLION);
        if (abs >= THOUSAND) return String.format("%.0fK", value / THOUSAND);
        return String.format("%.0f", value);
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }
}
