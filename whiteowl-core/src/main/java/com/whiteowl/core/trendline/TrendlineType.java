package com.whiteowl.core.trendline;

public enum TrendlineType {

    RESISTANCE("Resistance"),
    SUPPORT("Support");

    private final String displayLabel;

    TrendlineType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

}
