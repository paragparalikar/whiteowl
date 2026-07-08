package com.whiteowl.workbench.charting.drawing;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public final class DrawingManager {

    private static final Color DEFAULT_DRAWING_COLOR = Color.web("#e0e0e0");
    private static final int MAIN_CANVAS_ID = -1;

    @Getter private final List<Drawing> drawings = new ArrayList<>();
    @Getter @Setter private DrawingTool activeTool;
    @Getter @Setter private Color activeColor = DEFAULT_DRAWING_COLOR;
    private Drawing pendingDrawing;
    @Getter private Drawing dragDrawing;
    @Getter private int dragAnchorIndex;
    private long dragStartTimestamp;
    private double dragStartValue;
    @Setter private Runnable onChanged;

    public static int mainCanvasId() {
        return MAIN_CANVAS_ID;
    }

    public List<Drawing> getDrawingsForCanvas(int canvasId) {
        return drawings.stream()
                .filter(d -> d.getCanvasId() == canvasId)
                .toList();
    }

    public Drawing handleClick(long timestamp, double value, int canvasId, Runnable onComplete) {
        if (activeTool == null) return null;
        DrawingAnchor anchor = new DrawingAnchor(timestamp, value);
        if (pendingDrawing == null) {
            pendingDrawing = new Drawing(activeTool, anchor, canvasId, activeColor);
            if (activeTool.getType().isTextInput()) {
                pendingDrawing.setText("");
            }
        } else if (pendingDrawing.getAnchor2() == null) {
            pendingDrawing.setAnchor2(anchor);
        } else {
            pendingDrawing.setAnchor3(anchor);
        }
        if (pendingDrawing.isComplete()) {
            return finishDrawing(onComplete);
        }
        return pendingDrawing;
    }

    public DrawingAnchor buildPreviewAnchor(long timestamp, double value) {
        return new DrawingAnchor(timestamp, value);
    }

    public Drawing getPendingDrawing() {
        return pendingDrawing;
    }

    public void cancelPending() {
        pendingDrawing = null;
    }

    public void clearActiveTool() {
        activeTool = null;
        pendingDrawing = null;
    }

    public void removeDrawing(Drawing drawing) {
        drawings.remove(drawing);
        notifyChanged();
    }

    public void loadDrawings(List<Drawing> loaded) {
        drawings.clear();
        pendingDrawing = null;
        dragDrawing = null;
        drawings.addAll(loaded);
    }

    public Drawing findDrawingAt(double pixelX, double pixelY, int canvasId,
                                 CoordinateMapper mapper) {
        for (Drawing d : drawings) {
            if (d.getCanvasId() != canvasId) continue;
            if (hitTestDrawing(d, pixelX, pixelY, mapper) >= 0) {
                return d;
            }
        }
        return null;
    }

    public record HitResult(Drawing drawing, int anchorIndex) {}

    public HitResult findHitAt(double pixelX, double pixelY, int canvasId, CoordinateMapper mapper) {
        for (Drawing d : drawings) {
            if (d.getCanvasId() != canvasId) continue;
            int hitAnchor = hitTestDrawing(d, pixelX, pixelY, mapper);
            if (hitAnchor >= 0) {
                return new HitResult(d, hitAnchor);
            }
        }
        return null;
    }

    public boolean tryStartDrag(double pixelX, double pixelY, int canvasId,
                                long timestamp, double value,
                                CoordinateMapper mapper) {
        HitResult hit = findHitAt(pixelX, pixelY, canvasId, mapper);
        if (hit != null) {
            dragDrawing = hit.drawing;
            dragAnchorIndex = hit.anchorIndex;
            dragStartTimestamp = timestamp;
            dragStartValue = value;
            return true;
        }
        return false;
    }

    public void updateDrag(long timestamp, double value) {
        if (dragDrawing == null) return;
        if (dragAnchorIndex == 0) {
            updateBodyDrag(timestamp, value);
            return;
        }
        DrawingAnchor newAnchor = new DrawingAnchor(timestamp, value);
        switch (dragAnchorIndex) {
            case 1 -> dragDrawing.setAnchor1(newAnchor);
            case 2 -> dragDrawing.setAnchor2(newAnchor);
            case 3 -> dragDrawing.setAnchor3(newAnchor);
        }
    }

    public void endDrag() {
        dragDrawing = null;
        notifyChanged();
    }

    public boolean isDragging() {
        return dragDrawing != null;
    }

    public void clearAll() {
        drawings.clear();
        pendingDrawing = null;
        dragDrawing = null;
        notifyChanged();
    }

    private int hitTestDrawing(Drawing d, double px, double py, CoordinateMapper mapper) {
        double ax1 = mapper.toX(d.getAnchor1().getTimestamp());
        double ay1 = mapper.toY(d.getAnchor1().getValue());
        return d.getTool().getType().hitTest(d, px, py, mapper, ax1, ay1);
    }

    private void updateBodyDrag(long timestamp, double value) {
        long dTs = timestamp - dragStartTimestamp;
        double dVal = value - dragStartValue;
        dragDrawing.setAnchor1(shiftAnchor(dragDrawing.getAnchor1(), dTs, dVal));
        if (dragDrawing.getAnchor2() != null) {
            dragDrawing.setAnchor2(shiftAnchor(dragDrawing.getAnchor2(), dTs, dVal));
        }
        if (dragDrawing.getAnchor3() != null) {
            dragDrawing.setAnchor3(shiftAnchor(dragDrawing.getAnchor3(), dTs, dVal));
        }
        dragStartTimestamp = timestamp;
        dragStartValue = value;
    }

    private DrawingAnchor shiftAnchor(DrawingAnchor a, long dTs, double dVal) {
        return new DrawingAnchor(a.getTimestamp() + dTs, a.getValue() + dVal);
    }

    private Drawing finishDrawing(Runnable onComplete) {
        Drawing completed = pendingDrawing;
        drawings.add(completed);
        pendingDrawing = null;
        activeTool = null;
        notifyChanged();
        if (onComplete != null) onComplete.run();
        return completed;
    }

    private void notifyChanged() {
        if (onChanged != null) onChanged.run();
    }

}
