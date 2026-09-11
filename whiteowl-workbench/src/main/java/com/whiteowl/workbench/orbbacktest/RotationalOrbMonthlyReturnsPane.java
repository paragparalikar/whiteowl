package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.RotationalBacktestReport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.util.TreeMap;

/**
 * Monthly returns heatmap rendered on a Canvas.
 * Rows = years, columns = months. Each cell is color-coded by return magnitude.
 */
public final class RotationalOrbMonthlyReturnsPane extends VBox {

    private static final String PANE_STYLE = "rot-orb-monthly-pane";

    // Colors
    private static final Color CANVAS_BG = Color.web("#1e1f22");
    private static final Color HEADER_TEXT = Color.web("#6e7681");
    private static final Color CELL_TEXT = Color.web("#e0e0e0");
    private static final Color CELL_BORDER = Color.web("#333333");
    private static final Color POSITIVE_BASE = Color.web("#4ec9b0");
    private static final Color NEGATIVE_BASE = Color.web("#ef5350");
    private static final Color NEUTRAL_BG = Color.web("#2b2d30");

    // Layout
    private static final double CELL_WIDTH = 55;
    private static final double CELL_HEIGHT = 22;
    private static final double YEAR_COL_WIDTH = 50;
    private static final double ANNUAL_COL_WIDTH = 65;
    private static final double HEADER_HEIGHT = 24;
    private static final double FONT_SIZE = 10;
    private static final double HEADER_FONT_SIZE = 10;
    private static final int CONTENT_PADDING = 10;

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final Canvas canvas;
    private Map<Integer, Map<Integer, Double>> monthlyReturns;

    public RotationalOrbMonthlyReturnsPane() {
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
        this.monthlyReturns = report != null ? report.getMonthlyReturns() : null;
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

        if (monthlyReturns == null || monthlyReturns.isEmpty()) return;

        // Title
        gc.setFill(HEADER_TEXT);
        gc.setFont(Font.font("Montserrat SemiBold", 11));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Monthly Returns (%)", 4, 14);

        double startY = 20;

        // Sort years
        TreeMap<Integer, Map<Integer, Double>> sorted = new TreeMap<>(monthlyReturns);

        // Column headers
        gc.setFont(Font.font(HEADER_FONT_SIZE));
        gc.setFill(HEADER_TEXT);
        gc.setTextAlign(TextAlignment.CENTER);

        gc.fillText("Year", YEAR_COL_WIDTH / 2, startY + HEADER_HEIGHT - 6);
        for (int m = 0; m < 12; m++) {
            double x = YEAR_COL_WIDTH + m * CELL_WIDTH + CELL_WIDTH / 2;
            gc.fillText(MONTHS[m], x, startY + HEADER_HEIGHT - 6);
        }
        gc.fillText("Annual", YEAR_COL_WIDTH + 12 * CELL_WIDTH + ANNUAL_COL_WIDTH / 2,
                startY + HEADER_HEIGHT - 6);

        // Rows
        double y = startY + HEADER_HEIGHT;
        gc.setFont(Font.font(FONT_SIZE));

        for (Map.Entry<Integer, Map<Integer, Double>> yearEntry : sorted.entrySet()) {
            int year = yearEntry.getKey();
            Map<Integer, Double> months = yearEntry.getValue();

            // Year label
            gc.setFill(CELL_TEXT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(year), YEAR_COL_WIDTH / 2, y + CELL_HEIGHT - 6);

            double annualReturn = 1.0;

            for (int m = 1; m <= 12; m++) {
                double x = YEAR_COL_WIDTH + (m - 1) * CELL_WIDTH;
                Double ret = months.get(m);

                if (ret != null) {
                    annualReturn *= (1 + ret);
                    drawCell(gc, x, y, CELL_WIDTH, CELL_HEIGHT, ret);
                } else {
                    // Empty cell
                    gc.setFill(NEUTRAL_BG);
                    gc.fillRect(x + 0.5, y + 0.5, CELL_WIDTH - 1, CELL_HEIGHT - 1);
                    gc.setStroke(CELL_BORDER);
                    gc.setLineWidth(0.5);
                    gc.strokeRect(x + 0.5, y + 0.5, CELL_WIDTH - 1, CELL_HEIGHT - 1);
                    gc.setFill(HEADER_TEXT);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("-", x + CELL_WIDTH / 2, y + CELL_HEIGHT - 6);
                }
            }

            // Annual column
            double annualRet = annualReturn - 1.0;
            double annualX = YEAR_COL_WIDTH + 12 * CELL_WIDTH;
            drawCell(gc, annualX, y, ANNUAL_COL_WIDTH, CELL_HEIGHT, annualRet);

            y += CELL_HEIGHT;
        }
    }

    private void drawCell(GraphicsContext gc, double x, double y, double w, double h, double returnValue) {
        Color bg = cellColor(returnValue);
        gc.setFill(bg);
        gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
        gc.setStroke(CELL_BORDER);
        gc.setLineWidth(0.5);
        gc.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);

        // Text
        String text = String.format("%.1f", returnValue * 100);
        gc.setFill(CELL_TEXT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x + w / 2, y + h - 6);
    }

    /**
     * Map return to color: negative = red shade, positive = green shade.
     * Saturation scales with magnitude up to ~10% monthly.
     */
    private Color cellColor(double ret) {
        double magnitude = Math.min(Math.abs(ret) / 0.10, 1.0); // cap at 10%
        if (ret > 0) {
            return Color.color(
                    0.12 + 0.06 * (1 - magnitude),
                    0.20 + 0.40 * magnitude,
                    0.15 + 0.30 * magnitude,
                    0.4 + 0.5 * magnitude);
        } else if (ret < 0) {
            return Color.color(
                    0.35 + 0.55 * magnitude,
                    0.10 + 0.05 * (1 - magnitude),
                    0.10 + 0.05 * (1 - magnitude),
                    0.4 + 0.5 * magnitude);
        } else {
            return NEUTRAL_BG;
        }
    }
}
