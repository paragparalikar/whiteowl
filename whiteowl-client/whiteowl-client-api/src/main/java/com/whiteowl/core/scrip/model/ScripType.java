package com.whiteowl.core.scrip.model;

import lombok.Getter;

@Getter
public enum ScripType {

    EQUITY("Equity"),
    INDEX("Index"),
    FUTURES("Futures"),
    OPTIONS("Options"),
    ETF("ETF"),
    CURRENCY("Currency"),
    DEBT("Debt"),
    OTHER("Other");

    private final String displayLabel;

    ScripType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

}
