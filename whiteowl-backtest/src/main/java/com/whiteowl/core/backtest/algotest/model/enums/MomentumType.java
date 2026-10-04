package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MomentumType implements AlgoTestEnum {
    POINTS_UP("PointsUp"), POINTS_DOWN("PointsDown"), PERCENTAGE_UP("PercentageUp"), PERCENTAGE_DOWN("PercentageDown"), UNDERLYING_POINTS_UP("UnderlyingPointsUp"), UNDERLYING_POINTS_DOWN("UnderlyingPointsDown"), UNDERLYING_PERCENTAGE_UP("UnderlyingPercentageUp"), UNDERLYING_PERCENTAGE_DOWN("UnderlyingPercentageDown"), DELAYED_BREADTH_HIGH("DelayedBreadthHigh"), DELAYED_BREADTH_LOW("DelayedBreadthLow"), DELAYED_UNDERLYING_BREADTH_HIGH("DelayedUnderlyingBreadthHigh"), DELAYED_UNDERLYING_BREADTH_LOW("DelayedUnderlyingBreadthLow");

    private final String apiName;

    MomentumType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "MomentumType." + apiName;
    }

    @JsonCreator
    public static MomentumType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (MomentumType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown MomentumType value: " + value);
    }
}
