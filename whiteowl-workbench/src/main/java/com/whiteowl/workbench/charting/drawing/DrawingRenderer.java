package com.whiteowl.workbench.charting.drawing;

import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.LINE_WIDTH;

import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public final class DrawingRenderer {

    private DrawingRenderer() {
    }

    public static void renderDrawings(GraphicsContext gc, List<Drawing> drawings,
                                      CoordinateMapper mapper, Drawing pending,
                                      DrawingAnchor previewAnchor) {
        for (Drawing d : drawings) {
            renderOne(gc, d, mapper, null);
        }
        if (pending != null && !pending.isComplete() && previewAnchor != null) {
            renderOne(gc, pending, mapper, previewAnchor);
        }
    }

    private static void renderOne(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                                  DrawingAnchor preview) {
        gc.setLineWidth(LINE_WIDTH);
        gc.setLineDashes();
        gc.setGlobalAlpha(1.0);
        gc.setStroke(d.getColor());
        gc.setFill(d.getColor());
        double x1 = mapper.toX(d.getAnchor1().getTimestamp());
        double y1 = mapper.toY(d.getAnchor1().getValue());
        DrawingAnchor resolvedA2 = resolveAnchor(d.getAnchor2(), d, preview, 2);
        DrawingAnchor resolvedA3 = resolveAnchor(d.getAnchor3(), d, preview, 3);
        double x2 = resolvedA2 != null ? mapper.toX(resolvedA2.getTimestamp()) : x1;
        double y2 = resolvedA2 != null ? mapper.toY(resolvedA2.getValue()) : y1;
        double x3 = resolvedA3 != null ? mapper.toX(resolvedA3.getTimestamp()) : x1;
        double y3 = resolvedA3 != null ? mapper.toY(resolvedA3.getValue()) : y1;
        d.getTool().getType().render(gc, d, mapper, x1, y1, x2, y2, x3, y3);
    }

    private static DrawingAnchor resolveAnchor(DrawingAnchor existing, Drawing d,
                                               DrawingAnchor preview, int anchorNum) {
        if (existing != null) return existing;
        if (preview == null) return null;
        int nextNeeded = d.getClickCount() + 1;
        return nextNeeded == anchorNum ? preview : null;
    }

}
