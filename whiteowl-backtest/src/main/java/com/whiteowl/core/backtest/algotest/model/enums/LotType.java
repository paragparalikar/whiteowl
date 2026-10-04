package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LotType implements AlgoTestEnum {
    QUANTITY("Quantity"), CAPITAL("Capital");

    private final String apiName;

    LotType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "LotType." + apiName;
    }

    @JsonCreator
    public static LotType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (LotType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LotType value: " + value);
    }
}
