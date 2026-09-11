package com.whiteowl.workbench;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public final class WindowResizeHandler {

    private static final int RESIZE_MARGIN = 6;

    private final Stage stage;
    private final Scene scene;
    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;
    private double startStageX;
    private double startStageY;
    private Cursor resizeCursor = Cursor.DEFAULT;

    public WindowResizeHandler(Stage stage) {
        this.stage = stage;
        this.scene = stage.getScene();
        attachHandlers();
    }

    private void attachHandlers() {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::updateCursor);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePressed);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDragged);
    }

    private void updateCursor(MouseEvent event) {
        resizeCursor = computeCursor(event.getX(), event.getY());
        scene.setCursor(resizeCursor);
    }

    private void handlePressed(MouseEvent event) {
        resizeCursor = computeCursor(event.getX(), event.getY());
        if (Cursor.DEFAULT == resizeCursor) return;
        startX = event.getScreenX();
        startY = event.getScreenY();
        startWidth = stage.getWidth();
        startHeight = stage.getHeight();
        startStageX = stage.getX();
        startStageY = stage.getY();
        event.consume();
    }

    private void handleDragged(MouseEvent event) {
        if (Cursor.DEFAULT == resizeCursor) return;
        double dx = event.getScreenX() - startX;
        double dy = event.getScreenY() - startY;
        resizeByDirection(dx, dy);
        event.consume();
    }

    private void resizeByDirection(double dx, double dy) {
        if (isRight()) {
            applyWidth(startWidth + dx);
        }
        if (isBottom()) {
            applyHeight(startHeight + dy);
        }
        if (isLeft()) {
            applyLeftResize(dx);
        }
        if (isTop()) {
            applyTopResize(dy);
        }
    }

    private void applyWidth(double newWidth) {
        double clamped = Math.max(newWidth, stage.getMinWidth());
        stage.setWidth(clamped);
    }

    private void applyHeight(double newHeight) {
        double clamped = Math.max(newHeight, stage.getMinHeight());
        stage.setHeight(clamped);
    }

    private void applyLeftResize(double dx) {
        double newWidth = startWidth - dx;
        if (newWidth >= stage.getMinWidth()) {
            stage.setWidth(newWidth);
            stage.setX(startStageX + dx);
        }
    }

    private void applyTopResize(double dy) {
        double newHeight = startHeight - dy;
        if (newHeight >= stage.getMinHeight()) {
            stage.setHeight(newHeight);
            stage.setY(startStageY + dy);
        }
    }

    private Cursor computeCursor(double x, double y) {
        double w = scene.getWidth();
        double h = scene.getHeight();
        boolean top = y < RESIZE_MARGIN;
        boolean bottom = y > h - RESIZE_MARGIN;
        boolean left = x < RESIZE_MARGIN;
        boolean right = x > w - RESIZE_MARGIN;
        if (top && left) return Cursor.NW_RESIZE;
        if (top && right) return Cursor.NE_RESIZE;
        if (bottom && left) return Cursor.SW_RESIZE;
        if (bottom && right) return Cursor.SE_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;
        return Cursor.DEFAULT;
    }

    private boolean isRight() {
        return Cursor.E_RESIZE == resizeCursor || Cursor.NE_RESIZE == resizeCursor || Cursor.SE_RESIZE == resizeCursor;
    }

    private boolean isLeft() {
        return Cursor.W_RESIZE == resizeCursor || Cursor.NW_RESIZE == resizeCursor || Cursor.SW_RESIZE == resizeCursor;
    }

    private boolean isTop() {
        return Cursor.N_RESIZE == resizeCursor || Cursor.NE_RESIZE == resizeCursor || Cursor.NW_RESIZE == resizeCursor;
    }

    private boolean isBottom() {
        return Cursor.S_RESIZE == resizeCursor || Cursor.SE_RESIZE == resizeCursor || Cursor.SW_RESIZE == resizeCursor;
    }

}
