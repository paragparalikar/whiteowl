package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class InfoBarLayer implements ChartLayer {

    private static final double OUTER_GAP = 12;
    private static final double INNER_GAP = 4;
    private static final String LABEL_OPEN = "O";
    private static final String LABEL_HIGH = "H";
    private static final String LABEL_LOW = "L";
    private static final String LABEL_CLOSE = "C";
    private static final String LABEL_VOLUME = "Vol";
    private static final String PERCENT_FORMAT = "%+.2f%%";
    private static final DateTimeFormatter INFO_BAR_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yy HH:mm");
    private static final DateTimeFormatter INFO_BAR_DATE_ONLY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final long INTRADAY_THRESHOLD_SECONDS = 86400L;

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        Scrip scrip = ctx.scrip();
        if (scrip == null) return;
        int hoveredIdx = resolveHoveredBarIndex(ctx);
        gc.setFont(Font.font(INFO_FONT_SIZE));
        double x = 8;
        double y = INFO_BAR_HEIGHT;
        x = drawSegment(gc, scrip.getName(), INFO_VALUE, x, y);
        x += OUTER_GAP;
        x = drawSegment(gc, scrip.getExchange().getCode(), INFO_LABEL, x, y);
        Bars bars = ctx.bars();
        if (hoveredIdx < 0 || hoveredIdx >= bars.size()) return;
        float open = bars.getOpen(hoveredIdx);
        float high = bars.getHigh(hoveredIdx);
        float low = bars.getLow(hoveredIdx);
        float close = bars.getClose(hoveredIdx);
        long volume = bars.getVolume(hoveredIdx);
        long timestamp = bars.getTimestamp(hoveredIdx);
        boolean isBullish = close >= open;
        Color valueColor = isBullish ? BULLISH : BEARISH;
        float pctChange = open != 0 ? ((close - open) / open) * 100f : 0f;
        x += OUTER_GAP;
        x = drawSegment(gc, formatTimestamp(timestamp, ctx.timeframe().getSeconds()), INFO_LABEL, x, y);
        x += OUTER_GAP;
        x = drawLabel(gc, LABEL_OPEN, x, y);
        x += INNER_GAP;
        x = drawSegment(gc, formatPrice(open), valueColor, x, y);
        x += OUTER_GAP;
        x = drawLabel(gc, LABEL_HIGH, x, y);
        x += INNER_GAP;
        x = drawSegment(gc, formatPrice(high), valueColor, x, y);
        x += OUTER_GAP;
        x = drawLabel(gc, LABEL_LOW, x, y);
        x += INNER_GAP;
        x = drawSegment(gc, formatPrice(low), valueColor, x, y);
        x += OUTER_GAP;
        x = drawLabel(gc, LABEL_CLOSE, x, y);
        x += INNER_GAP;
        x = drawSegment(gc, formatPrice(close), valueColor, x, y);
        x += OUTER_GAP;
        x = drawLabel(gc, LABEL_VOLUME, x, y);
        x += INNER_GAP;
        x = drawSegment(gc, formatVolume(volume), INFO_VALUE, x, y);
        x += OUTER_GAP;
        drawSegment(gc, String.format(PERCENT_FORMAT, pctChange), valueColor, x, y);
    }

    private int resolveHoveredBarIndex(ChartContext ctx) {
        if (ctx.mouseX() < 0 || ctx.mouseX() > ctx.chartWidth()) return -1;
        int offset = (int) (ctx.mouseX() / ctx.barWidth());
        int viewIndex = ctx.viewStart() + offset;
        return ctx.translateIndex(viewIndex);
    }

    private double drawSegment(GraphicsContext gc, String text, Color color, double x, double y) {
        gc.setFill(color);
        gc.fillText(text, x, y);
        return x + text.length() * INFO_FONT_SIZE * 0.58;
    }

    private double drawLabel(GraphicsContext gc, String label, double x, double y) {
        return drawSegment(gc, label, INFO_LABEL, x, y);
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

    private String formatVolume(long volume) {
        if (volume >= 10_000_000) return String.format("%.1fM", volume / 1_000_000.0);
        if (volume >= 100_000) return String.format("%.1fL", volume / 100_000.0);
        if (volume >= 1_000) return String.format("%.1fK", volume / 1_000.0);
        return String.valueOf(volume);
    }

    private String formatTimestamp(long epochMillis, long timeframeSeconds) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        DateTimeFormatter formatter = timeframeSeconds < INTRADAY_THRESHOLD_SECONDS
                ? INFO_BAR_DATE_FMT : INFO_BAR_DATE_ONLY_FMT;
        return instant.atZone(ZoneId.systemDefault()).format(formatter);
    }

}
