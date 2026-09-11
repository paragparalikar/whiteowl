package com.whiteowl.workbench.charting.drawing;

public interface CoordinateMapper {

    double toX(long timestamp);

    double toY(double value);

    double chartWidth();

    double chartHeight();

    default int countBars(long ts1, long ts2) {
        return -1;
    }

}
