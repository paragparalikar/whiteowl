package com.whiteowl.workbench.charting.drawing;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class DrawingRenderUtils {

    public static final double LINE_WIDTH = 1.0;
    public static final double DASH_LENGTH = 6;
    public static final double DASH_GAP = 4;
    public static final double TEXT_FONT_SIZE = 11;
    public static final double TRADE_LINE_WIDTH = 1.5;
    public static final double TRADE_OPACITY = 0.25;
    public static final double ANCHOR_RADIUS = 3;
    public static final double HIT_RADIUS_PX = 8;
    private static final double GRIP_DOT_RADIUS = 1.5;
    private static final double GRIP_DOT_SPACING = 5;
    public static final double GRIP_WIDTH = 14;
    public static final double GRIP_HEIGHT = 14;
    private static final double GRIP_BG_RADIUS = 3;
    private static final double GRIP_BG_OPACITY = 0.5;
    public static final double GRIP_MARGIN = 4;
    private static final Color GRIP_BG_COLOR = Color.web("#1e1f22");

    private DrawingRenderUtils() {
    }

    public static void drawAnchorDot(GraphicsContext gc, double x, double y) {
        gc.fillOval(x - ANCHOR_RADIUS, y - ANCHOR_RADIUS,
                ANCHOR_RADIUS * 2, ANCHOR_RADIUS * 2);
    }

    public static void drawMoveGrip(GraphicsContext gc, double x, double cy, Color color) {
        double gy = cy - GRIP_HEIGHT / 2;
        gc.setGlobalAlpha(GRIP_BG_OPACITY);
        gc.setFill(GRIP_BG_COLOR);
        gc.fillRoundRect(x, gy, GRIP_WIDTH, GRIP_HEIGHT, GRIP_BG_RADIUS * 2, GRIP_BG_RADIUS * 2);
        gc.setGlobalAlpha(0.8);
        gc.setFill(color);
        double dotCx1 = x + GRIP_WIDTH / 2 - GRIP_DOT_SPACING / 2;
        double dotCx2 = x + GRIP_WIDTH / 2 + GRIP_DOT_SPACING / 2;
        double dotCy1 = cy - GRIP_DOT_SPACING / 2;
        double dotCy2 = cy + GRIP_DOT_SPACING / 2;
        drawGripDot(gc, dotCx1, dotCy1);
        drawGripDot(gc, dotCx2, dotCy1);
        drawGripDot(gc, dotCx1, dotCy2);
        drawGripDot(gc, dotCx2, dotCy2);
        gc.setGlobalAlpha(1.0);
    }

    private static void drawGripDot(GraphicsContext gc, double cx, double cy) {
        gc.fillOval(cx - GRIP_DOT_RADIUS, cy - GRIP_DOT_RADIUS,
                GRIP_DOT_RADIUS * 2, GRIP_DOT_RADIUS * 2);
    }

    public static boolean hitTestPoint(double px, double py, double ax, double ay) {
        double dx = px - ax;
        double dy = py - ay;
        return dx * dx + dy * dy <= HIT_RADIUS_PX * HIT_RADIUS_PX;
    }

    public static boolean hitTestLineSegment(double px, double py,
                                      double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) return hitTestPoint(px, py, x1, y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        double dist = Math.sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY));
        return dist <= HIT_RADIUS_PX;
    }

}
