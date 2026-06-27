package com.whiteowl.workbench.charting.drawing;

import javafx.scene.canvas.GraphicsContext;

public interface DrawingType {

    int requiredClicks();

    String label();

    void render(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                double x1, double y1, double x2, double y2, double x3, double y3);

    int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                double ax1, double ay1);

    default boolean isTextInput() {
        return false;
    }

}
