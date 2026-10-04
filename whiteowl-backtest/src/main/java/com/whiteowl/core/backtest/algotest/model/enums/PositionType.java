package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PositionType implements AlgoTestEnum {
    BUY("Buy"), SELL("Sell");

    private final String apiName;

    PositionType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "PositionType." + apiName;
    }

    @JsonCreator
    public static PositionType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (PositionType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown PositionType value: " + value);
    }
}
