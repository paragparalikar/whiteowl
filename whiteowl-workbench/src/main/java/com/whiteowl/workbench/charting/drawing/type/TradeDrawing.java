package com.whiteowl.workbench.charting.drawing.type;

import static com.whiteowl.workbench.charting.ChartTheme.BEARISH;
import static com.whiteowl.workbench.charting.ChartTheme.BULLISH;
import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.*;

import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingType;
import javafx.scene.canvas.GraphicsContext;

public final class TradeDrawing implements DrawingType {

    private static final int REQUIRED_CLICKS = 3;
    private static final double BOX_PADDING = 2;

    private final String label;
    private final boolean isLong;

    public TradeDrawing(String label, boolean isLong) {
        this.label = label;
        this.isLong = isLong;
    }

    @Override
    public int requiredClicks() {
        return REQUIRED_CLICKS;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public void render(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                       double entryX, double entryY, double tpX, double tpY,
                       double slX, double slY) {
        double left = Math.min(entryX, Math.min(tpX, slX)) - BOX_PADDING;
        double right = Math.max(entryX, Math.max(tpX, slX)) + BOX_PADDING;
        double width = right - left;
        gc.setLineWidth(TRADE_LINE_WIDTH);
        gc.setStroke(isLong ? BULLISH : BEARISH);
        gc.strokeLine(left, entryY, right, entryY);
        gc.setGlobalAlpha(TRADE_OPACITY);
        gc.setFill(BULLISH);
        gc.fillRect(left, Math.min(entryY, tpY), width, Math.abs(tpY - entryY));
        if (d.getAnchor3() != null || d.getClickCount() >= REQUIRED_CLICKS) {
            gc.setFill(BEARISH);
            gc.fillRect(left, Math.min(entryY, slY), width, Math.abs(slY - entryY));
        }
        gc.setGlobalAlpha(1.0);
        gc.setLineWidth(LINE_WIDTH);
        gc.setFill(d.getColor());
        drawAnchorDot(gc, entryX, entryY);
        gc.setFill(BULLISH);
        drawAnchorDot(gc, tpX, tpY);
        if (d.getAnchor3() != null) {
            gc.setFill(BEARISH);
            drawAnchorDot(gc, slX, slY);
        }
        drawMoveGrip(gc, right + GRIP_MARGIN, entryY, d.getColor());
    }

    @Override
    public int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                       double ax1, double ay1) {
        if (hitTestPoint(px, py, ax1, ay1)) return 1;
        if (d.getAnchor2() != null) {
            double ax2 = mapper.toX(d.getAnchor2().getTimestamp());
            double ay2 = mapper.toY(d.getAnchor2().getValue());
            if (hitTestPoint(px, py, ax2, ay2)) return 2;
        }
        if (d.getAnchor3() != null) {
            double ax3 = mapper.toX(d.getAnchor3().getTimestamp());
            double ay3 = mapper.toY(d.getAnchor3().getValue());
            if (hitTestPoint(px, py, ax3, ay3)) return 3;
        }
        if (hitTestTradeBody(d, px, py, mapper)) return 0;
        return -1;
    }

    private boolean hitTestTradeBody(Drawing d, double px, double py, CoordinateMapper mapper) {
        if (d.getAnchor2() == null) return false;
        double x1 = mapper.toX(d.getAnchor1().getTimestamp());
        double y1 = mapper.toY(d.getAnchor1().getValue());
        double x2 = mapper.toX(d.getAnchor2().getTimestamp());
        double y2 = mapper.toY(d.getAnchor2().getValue());
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double top = Math.min(y1, y2);
        double bottom = Math.max(y1, y2);
        if (d.getAnchor3() != null) {
            double x3 = mapper.toX(d.getAnchor3().getTimestamp());
            double y3 = mapper.toY(d.getAnchor3().getValue());
            minX = Math.min(minX, x3);
            maxX = Math.max(maxX, x3);
            top = Math.min(top, y3);
            bottom = Math.max(bottom, y3);
        }
        double left = minX - BOX_PADDING;
        double right = maxX + BOX_PADDING;
        double gripLeft = right + GRIP_MARGIN;
        double gripTop = y1 - GRIP_HEIGHT / 2;
        if (px >= gripLeft && px <= gripLeft + GRIP_WIDTH
                && py >= gripTop && py <= gripTop + GRIP_HEIGHT) {
            return true;
        }
        return px >= left && px <= right && py >= top && py <= bottom;
    }

}
