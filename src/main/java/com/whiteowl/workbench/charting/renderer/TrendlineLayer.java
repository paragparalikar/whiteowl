package com.whiteowl.workbench.charting.renderer;

import com.whiteowl.core.trendline.Pivot;
import com.whiteowl.core.trendline.Trendline;
import com.whiteowl.core.trendline.TrendlineType;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_DASH;
import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_RESISTANCE;
import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_SUPPORT;
import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_TOUCH;
import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_TOUCH_RADIUS;
import static com.whiteowl.workbench.charting.ChartTheme.TRENDLINE_WIDTH;

public final class TrendlineLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        if (ctx.trendlines() == null || ctx.trendlines().isEmpty()) return;
        for (Trendline line : ctx.trendlines()) {
            renderLine(gc, ctx, line);
        }
    }

    private void renderLine(GraphicsContext gc, ChartContext ctx, Trendline line) {
        Color lineColor = line.getType() == TrendlineType.RESISTANCE ? TRENDLINE_RESISTANCE : TRENDLINE_SUPPORT;
        int viewOffset = Math.max(0, ctx.viewStart());
        int drawStart = line.getStartIndex() + viewOffset;
        int drawEnd = line.getEndIndex() + viewOffset;
        drawStart = Math.max(drawStart, ctx.viewStart());
        drawEnd = Math.min(drawEnd, ctx.viewEnd() - 1);
        if (drawStart > ctx.viewEnd() || drawEnd < ctx.viewStart()) return;
        double x1 = toX(drawStart, ctx);
        double y1 = ctx.toY(line.priceAt(drawStart - viewOffset));
        double x2 = toX(drawEnd, ctx);
        double y2 = ctx.toY(line.priceAt(drawEnd - viewOffset));
        gc.save();
        gc.setStroke(lineColor);
        gc.setLineWidth(TRENDLINE_WIDTH);
        gc.setLineDashes(TRENDLINE_DASH);
        gc.strokeLine(x1, y1, x2, y2);
        gc.setLineDashes((double[]) null);
        gc.restore();
        renderTouches(gc, ctx, line, viewOffset);
    }

    private void renderTouches(GraphicsContext gc, ChartContext ctx, Trendline line, int viewOffset) {
        gc.setFill(TRENDLINE_TOUCH);
        for (Pivot touch : line.getTouches()) {
            int viewIdx = touch.getIndex() + viewOffset;
            if (viewIdx < ctx.viewStart() || viewIdx >= ctx.viewEnd()) continue;
            double x = toX(viewIdx, ctx);
            double y = ctx.toY(line.priceAt(touch.getIndex()));
            gc.fillOval(x - TRENDLINE_TOUCH_RADIUS, y - TRENDLINE_TOUCH_RADIUS,
                    TRENDLINE_TOUCH_RADIUS * 2, TRENDLINE_TOUCH_RADIUS * 2);
        }
    }

    private double toX(int barIndex, ChartContext ctx) {
        return (barIndex - ctx.viewStart()) * ctx.barWidth() + ctx.barWidth() / 2;
    }

}
