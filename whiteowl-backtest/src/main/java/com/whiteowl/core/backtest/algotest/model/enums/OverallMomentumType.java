package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OverallMomentumType implements AlgoTestEnum {
    POINTS_UP("PointsUp"), POINTS_DOWN("PointsDown"), PERCENTAGE_UP("PercentageUp"), PERCENTAGE_DOWN("PercentageDown");

    private final String apiName;

    OverallMomentumType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "OverallMomentumType." + apiName;
    }

    @JsonCreator
    public static OverallMomentumType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (OverallMomentumType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown OverallMomentumType value: " + value);
    }
}
