package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Strike selection for legs entered by strike type. Declared in walking order
 * from deep ITM through ATM out to deep OTM so that ordinal-based ranges
 * (e.g. ATM to OTM10) iterate the way the AlgoTest UI presents them.
 */
public enum StrikeType implements AlgoTestEnum {
    ITM20, ITM19, ITM18, ITM17, ITM16, ITM15, ITM14, ITM13, ITM12, ITM11,
    ITM10, ITM9, ITM8, ITM7, ITM6, ITM5, ITM4, ITM3, ITM2, ITM1, ITM,
    ATM,
    OTM1, OTM2, OTM3, OTM4, OTM5, OTM6, OTM7, OTM8, OTM9, OTM10,
    OTM11, OTM12, OTM13, OTM14, OTM15, OTM16, OTM17, OTM18, OTM19, OTM20,
    OTM21, OTM22, OTM23, OTM24, OTM25, OTM26, OTM27, OTM28, OTM29, OTM30;

    @JsonValue
    public String getApiValue() {
        return "StrikeType." + name();
    }

    /** Returns all strike types between {@code from} and {@code to} inclusive, in walking order. */
    public static List<StrikeType> range(StrikeType from, StrikeType to) {
        if (from.ordinal() > to.ordinal()) {
            throw new IllegalArgumentException(from + " is after " + to + " in the strike chain");
        }
        List<StrikeType> result = new ArrayList<>(to.ordinal() - from.ordinal() + 1);
        for (int i = from.ordinal(); i <= to.ordinal(); i++) {
            result.add(values()[i]);
        }
        return result;
    }

    @JsonCreator
    public static StrikeType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (StrikeType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown StrikeType value: " + value);
    }
}
