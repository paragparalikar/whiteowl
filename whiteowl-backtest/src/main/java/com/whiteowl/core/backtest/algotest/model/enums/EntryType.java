package com.whiteowl.core.backtest.algotest.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntryType implements AlgoTestEnum {
    ENTRY_BY_STRIKE_TYPE("EntryByStrikeType"), ENTRY_BY_EXACT_STRIKE("EntryByExactStrike"), ENTRY_BY_DELTA("EntryByDelta"), ENTRY_BY_DELTA_RANGE("EntryByDeltaRange"), ENTRY_BY_PREMIUM("EntryByPremium"), ENTRY_BY_PREMIUM_GEQ("EntryByPremiumGEQ"), ENTRY_BY_PREMIUM_LEQ("EntryByPremiumLEQ"), ENTRY_BY_PREMIUM_RANGE("EntryByPremiumRange"), ENTRY_BY_PREMIUM_CLOSE_TO_STRADDLE("EntryByPremiumCloseToStraddle"), ENTRY_BY_STRADDLE_PRICE("EntryByStraddlePrice"), ENTRY_BY_ATM_MULTIPLIER("EntryByAtmMultiplier"), ENTRY_BY_STRIKE_TYPE_MULTIPLIER("EntryByStrikeTypeMultiplier"), ENTRY_BY_SYNTHETIC_FUTURE("EntryBySyntheticFuture"), ENTRY_BY_SIGNAL("EntryBySignal");

    private final String apiName;

    EntryType(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String getApiValue() {
        return "EntryType." + apiName;
    }

    @JsonCreator
    public static EntryType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (EntryType e : values()) {
            String suffix = e.getApiValue().contains(".")
                    ? e.getApiValue().substring(e.getApiValue().lastIndexOf('.') + 1)
                    : e.getApiValue();
            if (e.getApiValue().equalsIgnoreCase(value) || suffix.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown EntryType value: " + value);
    }
}
