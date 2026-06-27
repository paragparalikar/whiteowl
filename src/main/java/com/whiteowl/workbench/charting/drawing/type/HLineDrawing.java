package com.whiteowl.workbench.charting.drawing.type;

import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.*;

import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingType;
import javafx.scene.canvas.GraphicsContext;

public final class HLineDrawing implements DrawingType {

    private static final String LABEL = "H Line";
    private static final int REQUIRED_CLICKS = 1;

    @Override
    public int requiredClicks() {
        return REQUIRED_CLICKS;
    }

    @Override
    public String label() {
        return LABEL;
    }

    @Override
    public void render(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                       double x1, double y1, double x2, double y2, double x3, double y3) {
        gc.setLineDashes(DASH_LENGTH, DASH_GAP);
        gc.strokeLine(0, y1, mapper.chartWidth(), y1);
        gc.setLineDashes();
    }

    @Override
    public int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                       double ax1, double ay1) {
        if (Math.abs(py - ay1) <= HIT_RADIUS_PX) return 0;
        return -1;
    }

}
