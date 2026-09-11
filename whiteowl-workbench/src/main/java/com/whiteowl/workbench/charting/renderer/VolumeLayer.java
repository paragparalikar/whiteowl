package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;

public final class VolumeLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        Bars bars = ctx.bars();
        long maxVolume = findMaxVolume(bars, ctx);
        if (maxVolume <= 0) return;
        double volumeZoneHeight = ctx.chartHeight() * VOLUME_ZONE_RATIO;
        double volumeBase = PADDING_TOP + ctx.chartHeight();
        gc.setGlobalAlpha(VOLUME_OPACITY);
        for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
            int idx = ctx.translateIndex(i);
            if (idx < 0 || idx >= bars.size()) continue;
            long volume = bars.getVolume(idx);
            if (volume <= 0) continue;
            boolean isBullish = bars.getClose(idx) >= bars.getOpen(idx);
            gc.setFill(isBullish ? BULLISH : BEARISH);
            double volHeight = volumeZoneHeight * volume / maxVolume;
            double x = (i - ctx.viewStart()) * ctx.barWidth();
            double centerX = x + ctx.barWidth() / 2;
            gc.fillRect(centerX - ctx.bodyWidth() / 2, volumeBase - volHeight, ctx.bodyWidth(), volHeight);
        }
        gc.setGlobalAlpha(1.0);
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
