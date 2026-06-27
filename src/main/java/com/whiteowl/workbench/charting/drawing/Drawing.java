package com.whiteowl.workbench.charting.drawing;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class Drawing {

    private final DrawingTool tool;
    @Setter private DrawingAnchor anchor1;
    @Setter private DrawingAnchor anchor2;
    @Setter private DrawingAnchor anchor3;
    @Setter private String text;
    @Setter private Color color;
    private final int canvasId;

    public Drawing(DrawingTool tool, DrawingAnchor anchor1, int canvasId, Color color) {
        this.tool = tool;
        this.anchor1 = anchor1;
        this.canvasId = canvasId;
        this.color = color;
    }

    public int getClickCount() {
        if (anchor3 != null) return 3;
        if (anchor2 != null) return 2;
        return 1;
    }

    public boolean isComplete() {
        return getClickCount() >= tool.getRequiredClicks();
    }

}
