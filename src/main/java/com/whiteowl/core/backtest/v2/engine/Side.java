package com.whiteowl.core.backtest.v2.engine;

public enum Side {

    LONG,
    SHORT;

    public Side opposite() {
        return this == LONG ? SHORT : LONG;
    }

}
