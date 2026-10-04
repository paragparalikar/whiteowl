package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorType implements AlgoTestEnum {
    TIME_INDICATOR("TimeIndicator"), POSITIONAL_INDICATOR("PositionalIndicator");

    private final String apiName;

    IndicatorType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "IndicatorType." + apiName;
    }

    @JsonCreator
    public static IndicatorType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (IndicatorType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown IndicatorType value: " + value);
    }
}
