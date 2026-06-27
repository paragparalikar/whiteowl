package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public final class PriceAxisLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        gc.setFill(AXIS_TEXT);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        Iterable<Double> ticks;
        if (ctx.logScale() && ctx.minPrice() > 0) {
            ticks = GridLayer.computeLogTicks(ctx.minPrice(), ctx.maxPrice());
        } else {
            ticks = computeLinearTicks(ctx);
        }
        for (double price : ticks) {
            double y = ctx.toY(price);
            gc.fillText(formatPrice((float) price), ctx.chartWidth() + 2, y + 3);
        }
    }

    private java.util.List<Double> computeLinearTicks(ChartContext ctx) {
        java.util.List<Double> ticks = new java.util.ArrayList<>();
        double tickInterval = GridLayer.computeNiceInterval(ctx.minPrice(), ctx.maxPrice());
        double firstTick = Math.ceil(ctx.minPrice() / tickInterval) * tickInterval;
        for (double price = firstTick; price <= ctx.maxPrice(); price += tickInterval) {
            ticks.add(price);
        }
        return ticks;
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
