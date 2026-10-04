package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Ticker implements AlgoTestEnum {
    NIFTY, BANKNIFTY, FINNIFTY, MIDCPNIFTY, SENSEX, BANKEX, INDIAVIX;

    @JsonValue
    public String getApiValue() {
        return name();
    }

    @JsonCreator
    public static Ticker fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (Ticker e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown Ticker value: " + value);
    }
}
