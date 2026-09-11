package com.whiteowl.core.backtest.v2.engine;

public enum FillPrice {

    OPEN,
    HIGH,
    LOW,
    CLOSE,
    TYPICAL_PRICE;

    public float resolve(float open, float high, float low, float close) {
        return switch (this) {
            case OPEN -> open;
            case HIGH -> high;
            case LOW -> low;
            case CLOSE -> close;
            case TYPICAL_PRICE -> (high + low + close) / 3f;
        };
    }

}
