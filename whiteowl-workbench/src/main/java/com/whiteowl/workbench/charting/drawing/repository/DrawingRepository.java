package com.whiteowl.workbench.charting.drawing.repository;

import com.whiteowl.workbench.charting.drawing.Drawing;

import java.util.List;

public interface DrawingRepository {

    List<Drawing> loadDrawings(String scripId);

    void saveDrawings(String scripId, List<Drawing> drawings);

}
