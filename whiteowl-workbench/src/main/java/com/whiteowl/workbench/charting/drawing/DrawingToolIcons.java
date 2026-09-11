package com.whiteowl.workbench.charting.drawing;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;

public final class DrawingToolIcons {

    private static final double STROKE_WIDTH = 1.4;
    private static final Color ICON_COLOR = Color.web("#868a91");

    private static final String H_SEGMENT_PATH = "M 1 7 L 13 7 M 1 4 L 1 10 M 13 4 L 13 10";
    private static final String V_SEGMENT_PATH = "M 7 1 L 7 13 M 4 1 L 10 1 M 4 13 L 10 13";
    private static final String H_LINE_PATH = "M 0 7 L 14 7";
    private static final String V_LINE_PATH = "M 7 0 L 7 14";
    private static final String SLANT_SEGMENT_PATH = "M 2 12 L 12 2 M 2 10 L 2 12 L 4 12 M 12 2 L 12 4";
    private static final String SLANT_LINE_PATH = "M 0 14 L 14 0";
    private static final String TEXT_PATH = "M 3 3 L 11 3 M 7 3 L 7 12 M 5 12 L 9 12";
    private static final String RULER_PATH = "M 1 9 L 5 13 L 13 5 L 9 1 Z M 4 10 L 6 8 M 7 11 L 9 9 M 10 8 L 12 6";
    private static final String CLEAR_PATH = "M 3 3 L 11 11 M 11 3 L 3 11";

    private DrawingToolIcons() {
    }

    public static SVGPath createIcon(DrawingTool tool) {
        String path = switch (tool) {
            case H_SEGMENT -> H_SEGMENT_PATH;
            case V_SEGMENT -> V_SEGMENT_PATH;
            case SLANT_SEGMENT -> SLANT_SEGMENT_PATH;
            case H_LINE -> H_LINE_PATH;
            case V_LINE -> V_LINE_PATH;
            case SLANT_LINE -> SLANT_LINE_PATH;
            case TEXT -> TEXT_PATH;
            case RULER -> RULER_PATH;
        };
        return buildSvgPath(path);
    }

    public static SVGPath createClearIcon() {
        return buildSvgPath(CLEAR_PATH);
    }

    private static SVGPath buildSvgPath(String content) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setStroke(ICON_COLOR);
        svg.setStrokeWidth(STROKE_WIDTH);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setFill(Color.TRANSPARENT);
        svg.getStyleClass().add("drawing-icon");
        return svg;
    }

}
