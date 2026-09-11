package com.whiteowl.workbench.charting.renderer;

import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileBin;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileData;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import static com.whiteowl.workbench.charting.ChartTheme.*;

public final class VolumeProfileLayer implements ChartLayer {

    private static final double BIN_GAP = 1.0;
    private static final double EDGE_HIT_TOLERANCE = 6.0;
    private static final double EDGE_LINE_WIDTH = 1.5;
    private static final double[] EDGE_DASH = {6, 4};
    private static final Color EDGE_COLOR = Color.web("#2196f3", 0.6);
    private static final Font COUNT_FONT = Font.font("Montserrat", VOLUME_PROFILE_TEXT_SIZE);
    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;
    private static final String THOUSAND_SUFFIX = "K";
    private static final String MILLION_SUFFIX = "M";
    private static final String BILLION_SUFFIX = "B";

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        VolumeProfileData data = ctx.volumeProfile();
        if (data == null || data.getBins().isEmpty()) return;
        double leftX = barIndexToX(data.getFromBar(), ctx) + data.getOffsetX();
        double rightX = barIndexToX(data.getToBar(), ctx) + data.getOffsetX();
        double maxBarWidth = rightX - leftX;
        if (maxBarWidth <= 0) return;
        gc.save();
        gc.setFont(COUNT_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        for (VolumeProfileBin bin : data.getBins()) {
            renderBin(gc, ctx, bin, data.getMaxCount(), maxBarWidth, leftX);
        }
        gc.restore();
        renderEdges(gc, ctx, data);
    }

    private void renderBin(GraphicsContext gc, ChartContext ctx, VolumeProfileBin bin,
                           long maxCount, double maxBarWidth, double startX) {
        double yTop = ctx.toY(bin.getHighPrice());
        double yBottom = ctx.toY(bin.getLowPrice());
        double binHeight = yBottom - yTop;
        if (binHeight < BIN_GAP * 2) return;
        double ratio = (double) bin.getCount() / maxCount;
        double barWidth = ratio * maxBarWidth;
        boolean isMax = bin.getCount() == maxCount;
        gc.setFill(isMax ? VOLUME_PROFILE_MAX_FILL : VOLUME_PROFILE_FILL);
        gc.setStroke(isMax ? VOLUME_PROFILE_MAX_STROKE : VOLUME_PROFILE_STROKE);
        gc.setLineWidth(VOLUME_PROFILE_STROKE_WIDTH);
        gc.fillRect(startX, yTop + BIN_GAP, barWidth, binHeight - BIN_GAP * 2);
        gc.strokeRect(startX, yTop + BIN_GAP, barWidth, binHeight - BIN_GAP * 2);
        if (bin.getCount() > 0 && binHeight > VOLUME_PROFILE_TEXT_SIZE + 2) {
            gc.setFill(VOLUME_PROFILE_TEXT);
            double textX = startX + barWidth + 4;
            double textY = yTop + (binHeight + VOLUME_PROFILE_TEXT_SIZE) / 2;
            gc.fillText(formatCount(bin.getCount()), textX, textY);
        }
    }

    private void renderEdges(GraphicsContext gc, ChartContext ctx, VolumeProfileData data) {
        double leftX = barIndexToX(data.getFromBar(), ctx);
        double rightX = barIndexToX(data.getToBar(), ctx);
        gc.save();
        gc.setStroke(EDGE_COLOR);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes(EDGE_DASH);
        double y0 = PADDING_TOP;
        double y1 = PADDING_TOP + ctx.chartHeight();
        gc.strokeLine(leftX, y0, leftX, y1);
        gc.strokeLine(rightX, y0, rightX, y1);
        gc.restore();
    }

    private String formatCount(long count) {
        if (count >= BILLION) return String.format("%.1f%s", (double) count / BILLION, BILLION_SUFFIX);
        if (count >= MILLION) return String.format("%.1f%s", (double) count / MILLION, MILLION_SUFFIX);
        if (count >= THOUSAND) return String.format("%.1f%s", (double) count / THOUSAND, THOUSAND_SUFFIX);
        return String.valueOf(count);
    }

    public boolean isHit(double px, double py, ChartContext ctx) {
        VolumeProfileData data = ctx.volumeProfile();
        if (data == null || data.getBins().isEmpty()) return false;
        double leftX = barIndexToX(data.getFromBar(), ctx) + data.getOffsetX();
        double rightX = barIndexToX(data.getToBar(), ctx) + data.getOffsetX();
        double maxBarWidth = rightX - leftX;
        if (maxBarWidth <= 0) return false;
        for (VolumeProfileBin bin : data.getBins()) {
            double yTop = ctx.toY(bin.getHighPrice());
            double yBottom = ctx.toY(bin.getLowPrice());
            double barWidth = ((double) bin.getCount() / data.getMaxCount()) * maxBarWidth;
            if (px >= leftX && px <= leftX + barWidth && py >= yTop && py <= yBottom) {
                return true;
            }
        }
        return false;
    }

    public EdgeHit hitTestEdge(double px, ChartContext ctx) {
        VolumeProfileData data = ctx.volumeProfile();
        if (data == null || data.getBins().isEmpty()) return EdgeHit.NONE;
        double leftX = barIndexToX(data.getFromBar(), ctx);
        double rightX = barIndexToX(data.getToBar(), ctx);
        if (Math.abs(px - leftX) <= EDGE_HIT_TOLERANCE) return EdgeHit.LEFT;
        if (Math.abs(px - rightX) <= EDGE_HIT_TOLERANCE) return EdgeHit.RIGHT;
        return EdgeHit.NONE;
    }

    public static int xToBarIndex(double px, ChartContext ctx) {
        double barWidth = ctx.barWidth();
        if (barWidth <= 0) return ctx.viewStart();
        return ctx.viewStart() + (int) (px / barWidth);
    }

    private double barIndexToX(int barIndex, ChartContext ctx) {
        return (barIndex - ctx.viewStart()) * ctx.barWidth();
    }

    public enum EdgeHit {
        NONE, LEFT, RIGHT
    }

}
