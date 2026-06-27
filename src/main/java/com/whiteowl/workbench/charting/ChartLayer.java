package com.whiteowl.workbench.charting;

import javafx.scene.canvas.GraphicsContext;

@FunctionalInterface
public interface ChartLayer {

    void render(GraphicsContext gc, ChartContext ctx);

}
