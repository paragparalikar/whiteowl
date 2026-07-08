package com.whiteowl.workbench.charting;

import com.whiteowl.workbench.charting.drawing.Drawing;

@FunctionalInterface
public interface RulerAddButtonHandler {

    void handle(Drawing rulerDrawing, double screenX, double screenY);

}
