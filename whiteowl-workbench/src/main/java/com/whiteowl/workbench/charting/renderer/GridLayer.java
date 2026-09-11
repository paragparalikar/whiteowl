package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

public final class GridLayer implements ChartLayer {

    private static final int TARGET_GRID_LINES = 6;
    private static final double[] NICE_MULTIPLIERS = {1.0, 2.0, 2.5, 5.0};

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        gc.setStroke(GRID);
        gc.setLineWidth(0.5);
        if (ctx.logScale() && ctx.minPrice() > 0) {
            renderLogGrid(gc, ctx);
        } else {
            renderLinearGrid(gc, ctx);
        }
    }

    private void renderLinearGrid(GraphicsContext gc, ChartContext ctx) {
        double tickInterval = computeNiceInterval(ctx.minPrice(), ctx.maxPrice());
        double firstTick = Math.ceil(ctx.minPrice() / tickInterval) * tickInterval;
        for (double price = firstTick; price <= ctx.maxPrice(); price += tickInterval) {
            double y = ctx.toY(price);
            gc.strokeLine(0, y, ctx.chartWidth(), y);
        }
    }

    private void renderLogGrid(GraphicsContext gc, ChartContext ctx) {
        for (double price : computeLogTicks(ctx.minPrice(), ctx.maxPrice())) {
            double y = ctx.toY(price);
            gc.strokeLine(0, y, ctx.chartWidth(), y);
        }
    }

    static List<Double> computeLogTicks(float minPrice, float maxPrice) {
        List<Double> ticks = new ArrayList<>();
        double logMin = Math.log10(minPrice);
        double logMax = Math.log10(maxPrice);
        double logInterval = computeNiceInterval((float) logMin, (float) logMax);
        double firstLog = Math.ceil(logMin / logInterval) * logInterval;
        for (double logVal = firstLog; logVal <= logMax; logVal += logInterval) {
            double price = Math.pow(10, logVal);
            if (price >= minPrice && price <= maxPrice) {
                ticks.add(price);
            }
        }
        if (ticks.size() < 3) {
            ticks.clear();
            double linearInterval = computeNiceInterval(minPrice, maxPrice);
            double firstTick = Math.ceil(minPrice / linearInterval) * linearInterval;
            for (double price = firstTick; price <= maxPrice; price += linearInterval) {
                ticks.add(price);
            }
        }
        return ticks;
    }

    static double computeNiceInterval(float minPrice, float maxPrice) {
        double range = maxPrice - minPrice;
        double roughInterval = range / TARGET_GRID_LINES;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughInterval)));
        double best = magnitude;
        for (double multiplier : NICE_MULTIPLIERS) {
            double candidate = magnitude * multiplier;
            if (Math.abs(range / candidate - TARGET_GRID_LINES) < Math.abs(range / best - TARGET_GRID_LINES)) {
                best = candidate;
            }
        }
        return best;
    }

}
