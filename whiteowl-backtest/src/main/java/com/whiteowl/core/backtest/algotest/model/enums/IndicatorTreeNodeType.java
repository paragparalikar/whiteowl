package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorTreeNodeType implements AlgoTestEnum {
    OPERAND_NODE("OperandNode"), DATA_NODE("DataNode");

    private final String apiName;

    IndicatorTreeNodeType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "IndicatorTreeNodeType." + apiName;
    }

    @JsonCreator
    public static IndicatorTreeNodeType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (IndicatorTreeNodeType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown IndicatorTreeNodeType value: " + value);
    }
}
