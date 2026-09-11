package com.whiteowl.workbench.charting.renderer;

import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import com.whiteowl.workbench.charting.TimeAxisRenderer;
import javafx.scene.canvas.GraphicsContext;

public final class TimeAxisLayer implements ChartLayer {

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        TimeAxisRenderer.draw(
                gc, ctx.bars(), ctx.dataProvider(),
                ctx.viewStart(), ctx.viewEnd(),
                ctx.chartWidth(), ctx.totalHeight(), ctx.barWidth()
        );
    }

}
