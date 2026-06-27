package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import com.whiteowl.workbench.charting.TimeAxisRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public final class CrosshairLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        double mx = ctx.mouseX();
        double my = ctx.mouseY();
        if (mx < 0 || mx > ctx.chartWidth() || my < PADDING_TOP || my > PADDING_TOP + ctx.chartHeight()) return;
        gc.setStroke(CROSSHAIR);
        gc.setLineWidth(0.5);
        gc.setLineDashes(4, 4);
        gc.strokeLine(mx, PADDING_TOP, mx, PADDING_TOP + ctx.chartHeight());
        gc.strokeLine(0, my, ctx.chartWidth(), my);
        gc.setLineDashes(null);
        drawPriceLabel(gc, ctx);
        drawTimeLabel(gc, ctx);
    }

    private void drawPriceLabel(GraphicsContext gc, ChartContext ctx) {
        float price = (float) ctx.toPrice(ctx.mouseY());
        String text = formatPrice(price);
        double labelW = text.length() * AXIS_FONT_SIZE * 0.65 + 6;
        double labelH = AXIS_FONT_SIZE + 6;
        double lx = ctx.chartWidth();
        double ly = ctx.mouseY() - labelH / 2;
        gc.setFill(CROSSHAIR_LABEL_BG);
        gc.fillRect(lx, ly, labelW, labelH);
        gc.setFill(CROSSHAIR_LABEL_TEXT);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        gc.fillText(text, lx + 3, ly + labelH - 3);
    }

    private void drawTimeLabel(GraphicsContext gc, ChartContext ctx) {
        int offset = (int) (ctx.mouseX() / ctx.barWidth());
        int viewIndex = ctx.viewStart() + offset;
        Bars bars = ctx.bars();
        int idx = ctx.translateIndex(viewIndex);
        if (idx < 0 || idx >= bars.size()) return;
        long ts = bars.getTimestamp(idx);
        String text = TimeAxisRenderer.formatForCrosshair(ts, ctx.timeframe().getSeconds());
        double labelW = text.length() * AXIS_FONT_SIZE * 0.65 + 6;
        double labelH = TIME_AXIS_HEIGHT;
        double lx = ctx.mouseX() - labelW / 2;
        double ly = ctx.totalHeight() - TIME_AXIS_HEIGHT;
        gc.setFill(CROSSHAIR_LABEL_BG);
        gc.fillRect(lx, ly, labelW, labelH);
        gc.setFill(CROSSHAIR_LABEL_TEXT);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        gc.fillText(text, lx + 3, ly + labelH - 3);
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
