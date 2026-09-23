package com.whiteowl.workbench.charting.renderer;

import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import com.whiteowl.workbench.charting.indicator.IndicatorResult.ExtraSeries;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class OverlayLayer implements ChartLayer {

    private static final double LINE_WIDTH = 0.5;

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        if (ctx.priceOverlayResults().isEmpty()) return;
        for (IndicatorResult result : ctx.priceOverlayResults()) {
            drawSeries(gc, ctx, result.getValues(), result.getColor(), result.getBarColors());
            for (ExtraSeries extra : result.getExtraSeries()) {
                drawSeries(gc, ctx, extra.getValues(), extra.getColor(), null);
            }
        }
    }

    private void drawSeries(GraphicsContext gc, ChartContext ctx, double[] data,
                            Color color, Color[] barColors) {
        boolean hasBarColors = barColors != null;
        gc.setStroke(color);
        gc.setLineWidth(LINE_WIDTH);
        double prevX = Double.NaN;
        double prevY = Double.NaN;
        for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
            int idx = ctx.translateIndex(i);
            if (idx < 0 || idx >= data.length) continue;
            double val = data[idx];
            if (Double.isNaN(val)) {
                prevX = Double.NaN;
                continue;
            }
            double x = (i - ctx.viewStart()) * ctx.barWidth() + ctx.barWidth() / 2;
            double y = ctx.toY(val);
            if (!Double.isNaN(prevX)) {
                if (hasBarColors && idx < barColors.length && barColors[idx] != null) {
                    gc.setStroke(barColors[idx]);
                }
                gc.strokeLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }
    }

}
