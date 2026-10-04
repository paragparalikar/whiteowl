package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReentryType implements AlgoTestEnum {
    IMMEDIATE("Immediate"), IMMEDIATE_REVERSE("ImmediateReverse"), AT_COST("AtCost"), AT_COST_REVERSE("AtCostReverse"), LIKE_ORIGINAL("LikeOriginal"), LIKE_ORIGINAL_REVERSE("LikeOriginalReverse"), NEXT_LEG("NextLeg");

    private final String apiName;

    ReentryType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "ReentryType." + apiName;
    }

    @JsonCreator
    public static ReentryType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (ReentryType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown ReentryType value: " + value);
    }
}
