package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OverallTgtSLType implements AlgoTestEnum {
    MTM("MTM"), PREMIUM_PERCENTAGE("PremiumPercentage");

    private final String apiName;

    OverallTgtSLType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "OverallTgtSLType." + apiName;
    }

    @JsonCreator
    public static OverallTgtSLType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (OverallTgtSLType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown OverallTgtSLType value: " + value);
    }
}
