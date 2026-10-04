package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StrategyType implements AlgoTestEnum {
    INTRADAY_SAME_DAY("IntradaySameDay"), INTRADAY_BTST("IntradayBTST");

    private final String apiName;

    StrategyType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "StrategyType." + apiName;
    }

    @JsonCreator
    public static StrategyType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (StrategyType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown StrategyType value: " + value);
    }
}
