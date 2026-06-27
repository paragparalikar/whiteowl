package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class CandlestickLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        Bars bars = ctx.bars();
        for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
            int idx = ctx.translateIndex(i);
            if (idx < 0 || idx >= bars.size()) continue;
            float open = bars.getOpen(idx);
            float close = bars.getClose(idx);
            float high = bars.getHigh(idx);
            float low = bars.getLow(idx);
            boolean isBullish = close >= open;
            Color color = isBullish ? BULLISH : BEARISH;
            double x = (i - ctx.viewStart()) * ctx.barWidth();
            double centerX = x + ctx.barWidth() / 2;
            double yHigh = ctx.toY(high);
            double yLow = ctx.toY(low);
            double yOpen = ctx.toY(open);
            double yClose = ctx.toY(close);
            gc.setStroke(color);
            gc.setLineWidth(WICK_WIDTH);
            gc.strokeLine(centerX, yHigh, centerX, yLow);
            double bodyTop = Math.min(yOpen, yClose);
            double bodyHeight = Math.max(1, Math.abs(yOpen - yClose));
            gc.setFill(color);
            gc.fillRect(centerX - ctx.bodyWidth() / 2, bodyTop, ctx.bodyWidth(), bodyHeight);
        }
    }

}
