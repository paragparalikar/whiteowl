package com.whiteowl.workbench.charting.drawing;

import com.whiteowl.workbench.charting.drawing.type.HLineDrawing;
import com.whiteowl.workbench.charting.drawing.type.HSegmentDrawing;
import com.whiteowl.workbench.charting.drawing.type.RulerDrawing;
import com.whiteowl.workbench.charting.drawing.type.SlantLineDrawing;
import com.whiteowl.workbench.charting.drawing.type.SlantSegmentDrawing;
import com.whiteowl.workbench.charting.drawing.type.TextDrawing;
import com.whiteowl.workbench.charting.drawing.type.VLineDrawing;
import com.whiteowl.workbench.charting.drawing.type.VSegmentDrawing;
import lombok.Getter;

@Getter
public enum DrawingTool {

    H_SEGMENT(new HSegmentDrawing()),
    V_SEGMENT(new VSegmentDrawing()),
    SLANT_SEGMENT(new SlantSegmentDrawing()),
    H_LINE(new HLineDrawing()),
    V_LINE(new VLineDrawing()),
    SLANT_LINE(new SlantLineDrawing()),
    TEXT(new TextDrawing()),
    RULER(new RulerDrawing());

    private final DrawingType type;

    DrawingTool(DrawingType type) {
        this.type = type;
    }

    public String getLabel() {
        return type.label();
    }

    public int getRequiredClicks() {
        return type.requiredClicks();
    }

}
