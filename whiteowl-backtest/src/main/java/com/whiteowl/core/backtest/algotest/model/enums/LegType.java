package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LegType implements AlgoTestEnum {
    CE, PE, FUT, FUT_P, CASH;

    @JsonValue
    public String getApiValue() {
        return "LegType." + name();
    }

    @JsonCreator
    public static LegType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (LegType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LegType value: " + value);
    }
}
