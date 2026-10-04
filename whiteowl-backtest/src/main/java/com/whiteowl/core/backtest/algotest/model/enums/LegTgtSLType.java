package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LegTgtSLType implements AlgoTestEnum {
    POINTS("Points"), PERCENTAGE("Percentage"), UNDERLYING_POINTS("UnderlyingPoints"), UNDERLYING_PERCENTAGE("UnderlyingPercentage"), DELTA_POINTS("DeltaPoints"), BREADTH_PLUS_POINTS("BreadthPlusPoints"), BREADTH_MINUS_POINTS("BreadthMinusPoints"), BREADTH_PLUS_PERCENTAGE("BreadthPlusPercentage"), BREADTH_MINUS_PERCENTAGE("BreadthMinusPercentage");

    private final String apiName;

    LegTgtSLType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "LegTgtSLType." + apiName;
    }

    @JsonCreator
    public static LegTgtSLType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (LegTgtSLType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LegTgtSLType value: " + value);
    }
}
