package com.whiteowl.core.backtest.dsl;

import com.whiteowl.core.bar.model.BarsArrays;

public enum FillPrice {

    OPEN,
    HIGH,
    LOW,
    CLOSE,
    TYPICAL_PRICE;

    private static final float THREE = 3f;

    public float resolve(BarsArrays arrays, int bar) {
        return switch (this) {
            case OPEN -> arrays.open()[bar];
            case HIGH -> arrays.high()[bar];
            case LOW -> arrays.low()[bar];
            case CLOSE -> arrays.close()[bar];
            case TYPICAL_PRICE -> (arrays.high()[bar] + arrays.low()[bar] + arrays.close()[bar]) / THREE;
        };
    }

}
