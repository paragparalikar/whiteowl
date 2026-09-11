package com.whiteowl.core.trendline;

public enum TrendlineDirection {

    ASCENDING("Ascending"),
    DESCENDING("Descending"),
    ANY("Any");

    private final String displayLabel;

    TrendlineDirection(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

}
