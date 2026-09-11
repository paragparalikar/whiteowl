package com.whiteowl.workbench.charting.drawing.type;

import static com.whiteowl.workbench.charting.drawing.DrawingRenderUtils.*;

import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public final class TextDrawing implements DrawingType {

    private static final String LABEL = "Text";
    private static final String DEFAULT_TEXT = "Text";
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
    public boolean isTextInput() {
        return true;
    }

    @Override
    public void render(GraphicsContext gc, Drawing d, CoordinateMapper mapper,
                       double x1, double y1, double x2, double y2, double x3, double y3) {
        gc.setFont(Font.font(TEXT_FONT_SIZE));
        String display = d.getText() != null && !d.getText().isEmpty() ? d.getText() : DEFAULT_TEXT;
        gc.fillText(display, x1, y1);
    }

    @Override
    public int hitTest(Drawing d, double px, double py, CoordinateMapper mapper,
                       double ax1, double ay1) {
        if (hitTestPoint(px, py, ax1, ay1)) return 0;
        return -1;
    }

}
