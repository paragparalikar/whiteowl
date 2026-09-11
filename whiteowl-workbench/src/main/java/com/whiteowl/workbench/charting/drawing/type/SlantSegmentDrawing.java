package com.whiteowl.workbench.charting.drawing.type;

import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.*;

import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingType;
import javafx.scene.canvas.GraphicsContext;

public final class SlantSegmentDrawing implements DrawingType {

    private static final String LABEL = "Segment";
    private static final int REQUIRED_CLICKS = 2;

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
        gc.strokeLine(x1, y1, x2, y2);
        drawAnchorDot(gc, x1, y1);
        drawAnchorDot(gc, x2, y2);
    }

    @Override
    public int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                       double ax1, double ay1) {
        if (d.getAnchor2() == null) return -1;
        double ax2 = mapper.toX(d.getAnchor2().getTimestamp());
        double ay2 = mapper.toY(d.getAnchor2().getValue());
        if (hitTestPoint(px, py, ax1, ay1)) return 1;
        if (hitTestPoint(px, py, ax2, ay2)) return 2;
        if (hitTestLineSegment(px, py, ax1, ay1, ax2, ay2)) return 0;
        return -1;
    }

}
