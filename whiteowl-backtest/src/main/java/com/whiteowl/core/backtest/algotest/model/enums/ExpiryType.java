package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExpiryType implements AlgoTestEnum {
    WEEKLY("Weekly"), NEXT_WEEKLY("NextWeekly"), MONTHLY("Monthly"), NEXT_MONTHLY("NextMonthly"), TODAY("Today"), TOMORROW("Tomorrow");

    private final String apiName;

    ExpiryType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "ExpiryType." + apiName;
    }

    @JsonCreator
    public static ExpiryType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (ExpiryType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown ExpiryType value: " + value);
    }
}
