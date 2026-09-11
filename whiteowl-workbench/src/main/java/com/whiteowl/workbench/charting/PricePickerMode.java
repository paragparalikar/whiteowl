package com.whiteowl.workbench.charting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PricePickerMode {

    BUY(false, false),
    SELL(false, false),
    GTT_BUY(true, false),
    GTT_SELL(true, false),
    OCO_FIRST(true, true),
    OCO_SECOND(true, true);

    private final boolean gtt;
    private final boolean oco;

}
