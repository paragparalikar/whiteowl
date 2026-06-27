package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;

public final class ScreenMarkerLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        boolean[] markers = ctx.screenMarkers();
        if (markers == null) return;
        Bars bars = ctx.bars();
        gc.setFill(SCREEN_MARKER);
        for (int i = ctx.viewStart(); i < ctx.viewEnd(); i++) {
            int idx = ctx.translateIndex(i);
            if (idx < 0 || idx >= bars.size() || idx >= markers.length || !markers[idx]) continue;
            float low = bars.getLow(idx);
            double x = (i - ctx.viewStart()) * ctx.barWidth();
            double centerX = x + ctx.barWidth() / 2;
            double yLow = ctx.toY(low);
            double tipY = yLow + SCREEN_MARKER_GAP;
            double baseY = tipY + SCREEN_MARKER_SIZE;
            double halfW = SCREEN_MARKER_SIZE / 2;
            gc.fillPolygon(
                    new double[]{centerX, centerX - halfW, centerX + halfW},
                    new double[]{tipY, baseY, baseY},
                    3);
        }
    }

}
