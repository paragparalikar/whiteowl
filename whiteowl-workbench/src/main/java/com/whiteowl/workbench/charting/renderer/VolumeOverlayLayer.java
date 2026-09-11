package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import javafx.scene.canvas.GraphicsContext;

public final class VolumeOverlayLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        if (ctx.volumeOverlayResults().isEmpty()) return;
        long maxVolume = findMaxVolume(ctx.bars(), ctx);
        if (maxVolume <= 0) return;
        double volumeZoneHeight = ctx.chartHeight() * VOLUME_ZONE_RATIO;
        double volumeBase = PADDING_TOP + ctx.chartHeight();
        for (IndicatorResult result : ctx.volumeOverlayResults()) {
            gc.setStroke(result.getColor());
            gc.setLineWidth(1.2);
            double prevX = Double.NaN;
            double prevY = Double.NaN;
            for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
                int idx = ctx.translateIndex(i);
                if (idx < 0 || idx >= result.getValues().length) continue;
                double val = result.getValues()[idx];
                if (Double.isNaN(val)) {
                    prevX = Double.NaN;
                    continue;
                }
                double x = (i - ctx.viewStart()) * ctx.barWidth() + ctx.barWidth() / 2;
                double y = volumeBase - volumeZoneHeight * val / maxVolume;
                if (!Double.isNaN(prevX)) {
                    gc.strokeLine(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    private long findMaxVolume(Bars bars, ChartContext ctx) {
        long max = 0;
        for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
            int idx = ctx.translateIndex(i);
            if (idx < 0 || idx >= bars.size()) continue;
            long vol = bars.getVolume(idx);
            if (vol > max) max = vol;
        }
        return max;
    }

}
