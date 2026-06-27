package com.whiteowl.workbench.charting.drawing.type;

import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.*;

import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public final class RulerDrawing implements DrawingType {

    private static final String LABEL = "Ruler";
    private static final int REQUIRED_CLICKS = 2;
    private static final double LABEL_FONT_SIZE = 10;
    private static final double LABEL_BG_PADDING_H = 6;
    private static final double LABEL_BG_PADDING_V = 4;
    private static final double LABEL_BG_RADIUS = 4;
    private static final double LABEL_OFFSET = 10;
    private static final double LABEL_BG_OPACITY = 0.85;
    private static final double LABEL_LINE_HEIGHT = 13;
    private static final String FONT_FAMILY = "Montserrat";
    private static final String BARS_SUFFIX = " bars";
    private static final String PERCENT_FORMAT = "%+.2f%%";
    private static final String PRICE_FORMAT = "%+.2f";
    private static final Color LABEL_BG_COLOR = Color.web("#1e1f22");
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 3600;
    private static final long SECONDS_PER_DAY = 86400;
    private static final String MINUTE_SUFFIX = "m";
    private static final String HOUR_SUFFIX = "h";
    private static final String DAY_SUFFIX = "d";
    private static final String SPACE = " ";
    private static final long MILLIS_PER_SECOND = 1000;

    @Override
    public int requiredClicks() {
        return REQUIRED_CLICKS;
    }

    @Override
    public String label() {
        return LABEL;
    }

    @Override
    public void render(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                       double x1, double y1, double x2, double y2, double x3, double y3) {
        gc.setLineDashes(DASH_LENGTH, DASH_GAP);
        gc.strokeLine(x1, y1, x2, y2);
        gc.setLineDashes();
        drawAnchorDot(gc, x1, y1);
        drawAnchorDot(gc, x2, y2);
        if (d.getAnchor2() != null) {
            renderInfoLabel(gc, d, mapper, x1, y1, x2, y2);
        }
    }

    private void renderInfoLabel(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                                 double x1, double y1, double x2, double y2) {
        double price1 = d.getAnchor1().getValue();
        double price2 = d.getAnchor2().getValue();
        long ts1 = d.getAnchor1().getTimestamp();
        long ts2 = d.getAnchor2().getTimestamp();
        double priceDiff = price2 - price1;
        double pctDiff = (price1 != 0) ? (priceDiff / price1) * 100 : 0;
        long spanSeconds = Math.abs(ts2 - ts1) / MILLIS_PER_SECOND;
        int barCount = mapper.countBars(ts1, ts2);
        String barsText = barCount + BARS_SUFFIX;
        String timespanText = formatTimespan(spanSeconds);
        String pctText = String.format(PERCENT_FORMAT, pctDiff);
        String priceText = String.format(PRICE_FORMAT, priceDiff);
        String[] lines = {barsText, timespanText, pctText, priceText};
        Font font = Font.font(FONT_FAMILY, LABEL_FONT_SIZE);
        gc.setFont(font);
        double maxLineWidth = measureMaxWidth(gc, lines);
        double bgWidth = maxLineWidth + LABEL_BG_PADDING_H * 2;
        double bgHeight = LABEL_LINE_HEIGHT * lines.length + LABEL_BG_PADDING_V * 2;
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        double bgX = midX + LABEL_OFFSET;
        double bgY = midY - bgHeight / 2;
        gc.setGlobalAlpha(LABEL_BG_OPACITY);
        gc.setFill(LABEL_BG_COLOR);
        gc.fillRoundRect(bgX, bgY, bgWidth, bgHeight, LABEL_BG_RADIUS * 2, LABEL_BG_RADIUS * 2);
        gc.setGlobalAlpha(1.0);
        gc.setFill(d.getColor());
        gc.setTextAlign(TextAlignment.LEFT);
        double textX = bgX + LABEL_BG_PADDING_H;
        double textY = bgY + LABEL_BG_PADDING_V + LABEL_FONT_SIZE;
        for (String line : lines) {
            gc.fillText(line, textX, textY);
            textY += LABEL_LINE_HEIGHT;
        }
    }

    private double measureMaxWidth(GraphicsContext gc, String[] lines) {
        double max = 0;
        for (String line : lines) {
            double w = computeTextWidth(gc.getFont(), line);
            if (w > max) max = w;
        }
        return max;
    }

    private double computeTextWidth(Font font, String text) {
        javafx.scene.text.Text helper = new javafx.scene.text.Text(text);
        helper.setFont(font);
        return helper.getLayoutBounds().getWidth();
    }

    private String formatTimespan(long seconds) {
        if (seconds < SECONDS_PER_HOUR) {
            return (seconds / SECONDS_PER_MINUTE) + MINUTE_SUFFIX;
        }
        if (seconds < SECONDS_PER_DAY) {
            long hours = seconds / SECONDS_PER_HOUR;
            long mins = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
            return mins > 0 ? hours + HOUR_SUFFIX + SPACE + mins + MINUTE_SUFFIX
                    : hours + HOUR_SUFFIX;
        }
        long days = seconds / SECONDS_PER_DAY;
        long hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        return hours > 0 ? days + DAY_SUFFIX + SPACE + hours + HOUR_SUFFIX
                : days + DAY_SUFFIX;
    }

    @Override
    public int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                       double ax1, double ay1) {
        if (d.getAnchor2() == null) return -1;
        double ax2 = mapper.toX(d.getAnchor2().getTimestamp());
        double ay2 = mapper.toY(d.getAnchor2().getValue());
        if (hitTestPoint(px, py, ax1, ay1)) return 1;
        if (hitTestPoint(px, py, ax2, ay2)) return 2;
        if (hitTestLineSegment(px, py, ax1, ay1, ax2, ay2)) return 0;
        return -1;
    }

}
