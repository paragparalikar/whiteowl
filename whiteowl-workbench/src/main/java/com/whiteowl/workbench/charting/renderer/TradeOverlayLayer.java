package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;

public final class TradeOverlayLayer implements ChartLayer {

    private List<TradeRecord> trades = Collections.emptyList();

    public void setTrades(List<TradeRecord> trades) {
        this.trades = trades != null ? trades : Collections.emptyList();
    }

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        if (trades.isEmpty()) return;
        Bars bars = ctx.bars();
        for (TradeRecord trade : trades) {
            renderTrade(gc, ctx, bars, trade);
        }
    }

    private void renderTrade(GraphicsContext gc, ChartContext ctx, Bars bars, TradeRecord trade) {
        int entryView = findViewIndex(bars, ctx, trade.getEntryTimestamp());
        int exitView = findViewIndex(bars, ctx, trade.getExitTimestamp());
        if (exitView < ctx.viewStart() || entryView >= ctx.viewEnd()) return;
        int clampedEntry = Math.max(entryView, ctx.viewStart());
        int clampedExit = Math.min(exitView, ctx.viewEnd() - 1);
        double entryX = (clampedEntry - ctx.viewStart()) * ctx.barWidth() + ctx.barWidth() / 2;
        double exitX = (clampedExit - ctx.viewStart()) * ctx.barWidth() + ctx.barWidth() / 2;
        double entryY = ctx.toY(trade.getEntryPrice());
        double exitY = ctx.toY(trade.getExitPrice());
        Color fill = pickFill(trade);
        Color stroke = pickStroke(trade);
        double rectX = Math.min(entryX, exitX);
        double rectW = Math.max(Math.abs(exitX - entryX), ctx.barWidth());
        double rectY = Math.min(entryY, exitY);
        double rectH = Math.max(Math.abs(exitY - entryY), 2);
        gc.setFill(fill);
        gc.fillRect(rectX, rectY, rectW, rectH);
        gc.setStroke(stroke);
        gc.setLineWidth(TRADE_STROKE_WIDTH);
        gc.strokeRect(rectX, rectY, rectW, rectH);
        renderEntryMarker(gc, entryX, entryY, trade.getSide(), stroke);
        renderExitMarker(gc, exitX, exitY, stroke);
    }

    private void renderEntryMarker(GraphicsContext gc, double x, double y, Side side, Color color) {
        gc.setFill(color);
        double s = TRADE_MARKER_SIZE;
        if (side == Side.LONG) {
            gc.fillPolygon(
                    new double[]{x, x - s, x + s},
                    new double[]{y - s, y + s, y + s},
                    3
            );
        } else {
            gc.fillPolygon(
                    new double[]{x, x - s, x + s},
                    new double[]{y + s, y - s, y - s},
                    3
            );
        }
    }

    private void renderExitMarker(GraphicsContext gc, double x, double y, Color color) {
        gc.setFill(color);
        double r = TRADE_MARKER_SIZE / 2.0;
        gc.fillOval(x - r, y - r, r * 2, r * 2);
    }

    private int findViewIndex(Bars bars, ChartContext ctx, long timestamp) {
        int cachedFrom = ctx.dataProvider().getCachedFrom();
        for (int i = 0; i < bars.size(); i++) {
            if (bars.getTimestamp(i) >= timestamp) {
                return cachedFrom + i;
            }
        }
        return cachedFrom + bars.size() - 1;
    }

    private Color pickFill(TradeRecord trade) {
        if (Math.abs(trade.getNetPnlPercent()) <= TRADE_BREAKEVEN_THRESHOLD) return TRADE_BREAKEVEN;
        return trade.getNetPnl() > 0 ? TRADE_WIN : TRADE_LOSS;
    }

    private Color pickStroke(TradeRecord trade) {
        if (Math.abs(trade.getNetPnlPercent()) <= TRADE_BREAKEVEN_THRESHOLD) return TRADE_BREAKEVEN_STROKE;
        return trade.getNetPnl() > 0 ? TRADE_WIN_STROKE : TRADE_LOSS_STROKE;
    }

}
