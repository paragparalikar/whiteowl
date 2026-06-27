package com.whiteowl.workbench.charting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PricePickerMode {

    BUY(false),
    SELL(false),
    GTT_BUY(true),
    GTT_SELL(true);

    private final boolean gtt;

}
