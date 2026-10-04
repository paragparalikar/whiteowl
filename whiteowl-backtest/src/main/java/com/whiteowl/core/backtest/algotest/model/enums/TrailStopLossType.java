package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TrailStopLossType implements AlgoTestEnum {
    POINTS("Points"), PERCENTAGE("Percentage"), DELTA_POINTS("DeltaPoints"), DELTA_PERCENTAGE("DeltaPercentage");

    private final String apiName;

    TrailStopLossType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "TrailStopLossType." + apiName;
    }

    @JsonCreator
    public static TrailStopLossType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (TrailStopLossType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown TrailStopLossType value: " + value);
    }
}
