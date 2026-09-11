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
 * Canvas-based drawdown chart for the rotational ORB backtest.
 * Designed to live in a SplitPane below the equity curve chart.
 */
public final class RotationalOrbDrawdownPane extends VBox {

    private static final String PANE_STYLE = "rot-orb-equity-pane";

    // Colors
    private static final Color DRAWDOWN_FILL = Color.web("#ef5350", 0.3);
    private static final Color DRAWDOWN_STROKE = Color.web("#ef5350", 0.6);
    private static final Color CANVAS_BG = Color.web("#1e1f22");
    private static final Color GRID_LINE = Color.web("#2b2d30");
    private static final Color AXIS_TEXT = Color.web("#868a91");
    private static final Color ZERO_LINE = Color.web("#3c3f41");

    // Layout
    private static final double LINE_WIDTH = 1.2;
    private static final double AXIS_FONT_SIZE = 9;
    private static final double PADDING_H = 50;
    private static final double PADDING_V = 12;
    private static final int GRID_LINES = 4;
    private static final int CONTENT_PADDING = 10;
    private static final int DATE_LABELS = 6;
    private static final double DATE_AXIS_HEIGHT = 18;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Canvas canvas;
    private RotationalBacktestReport report;

    public RotationalOrbDrawdownPane() {
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

        double[] dd = report.getDrawdownCurve();
        long[] dates = report.getDates();
        if (dd == null || dd.length < 2) return;

        renderDrawdown(gc, w, 0, h - DATE_AXIS_HEIGHT, dd, dates);
    }

    // ── Drawdown ─────────────────────────────────────────────────────

    private void renderDrawdown(GraphicsContext gc, double w, double top, double h,
                                double[] dd, long[] dates) {
        int size = dd.length;
        double min = Double.MAX_VALUE;
        for (double d : dd) min = Math.min(min, d);
        double max = 0;
        double range = max - min;
        if (range <= 0) range = 1;

        drawGrid(gc, top, w, h, min, max, range);
        drawDateAxis(gc, top + h, w, dates);

        double chartW = w - PADDING_H * 2;
        double chartH = h - PADDING_V * 2;
        double zeroY = top + PADDING_V;

        // Zero line
        gc.setStroke(ZERO_LINE);
        gc.setLineWidth(0.5);
        gc.strokeLine(PADDING_H, zeroY, w - PADDING_H, zeroY);

        // Filled area
        gc.setFill(DRAWDOWN_FILL);
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

        // Stroke line
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

    // ── Grid & Axes ──────────────────────────────────────────────────

    private void drawGrid(GraphicsContext gc, double top, double w, double h,
                          double min, double max, double range) {
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
            gc.fillText(String.format("%.1f%%", value * 100), 2, y + 3);
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
}
