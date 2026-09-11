package com.whiteowl.workbench.charting.drawing.repository;

import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingAnchor;
import com.whiteowl.workbench.charting.drawing.DrawingTool;
import javafx.scene.paint.Color;

import java.util.List;

public record DrawingDto(
        String tool,
        AnchorDto anchor1,
        AnchorDto anchor2,
        AnchorDto anchor3,
        String text,
        String color,
        int canvasId
) {

    public record AnchorDto(long timestamp, double value) {

        static AnchorDto fromAnchor(DrawingAnchor anchor) {
            if (anchor == null) return null;
            return new AnchorDto(anchor.getTimestamp(), anchor.getValue());
        }

        DrawingAnchor toAnchor() {
            return new DrawingAnchor(timestamp, value);
        }

    }

    public static DrawingDto fromDrawing(Drawing d) {
        return new DrawingDto(
                d.getTool().name(),
                AnchorDto.fromAnchor(d.getAnchor1()),
                AnchorDto.fromAnchor(d.getAnchor2()),
                AnchorDto.fromAnchor(d.getAnchor3()),
                d.getText(),
                toHex(d.getColor()),
                d.getCanvasId()
        );
    }

    public Drawing toDrawing() {
        DrawingTool drawingTool = DrawingTool.valueOf(tool);
        DrawingAnchor a1 = anchor1 != null ? anchor1.toAnchor() : null;
        Drawing d = new Drawing(drawingTool, a1, canvasId, Color.web(color));
        if (anchor2 != null) d.setAnchor2(anchor2.toAnchor());
        if (anchor3 != null) d.setAnchor3(anchor3.toAnchor());
        if (text != null) d.setText(text);
        return d;
    }

    public static List<DrawingDto> fromDrawings(List<Drawing> drawings) {
        return drawings.stream().map(DrawingDto::fromDrawing).toList();
    }

    public static List<Drawing> toDrawings(List<DrawingDto> dtos) {
        return dtos.stream().map(DrawingDto::toDrawing).toList();
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

}
