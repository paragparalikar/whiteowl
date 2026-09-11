package com.whiteowl.core.breadth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum for breadth formula selection in indicator settings dialogs.
 * The existing IndicatorSettingsDialog handles enum types as ComboBox dropdowns automatically.
 */
@Getter
@RequiredArgsConstructor
public enum BreadthFormulaType {

    ADVANCE_DECLINE("Advance/Decline"),
    PERCENTAGE("Percentage"),
    MONEY_FLOW("Money Flow");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static BreadthFormulaType fromDisplayName(String name) {
        for (BreadthFormulaType type : values()) {
            if (type.displayName.equals(name)) return type;
        }
        return ADVANCE_DECLINE;
    }
}
