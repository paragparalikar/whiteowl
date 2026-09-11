package com.whiteowl.core.rs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RSFormulaType {

    RS_LINE("RS Line"),
    MANSFIELD("Mansfield RS"),
    RS_RANK("RS Rank");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static RSFormulaType fromDisplayName(String name) {
        for (RSFormulaType type : values()) {
            if (type.displayName.equals(name)) return type;
        }
        return RS_LINE;
    }
}
