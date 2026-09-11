package com.whiteowl.core.backtest.v2.engine;

public enum SignalType {

    LONG_ENTRY,
    SHORT_ENTRY,
    LONG_EXIT,
    SHORT_EXIT,
    CLOSE_POSITION;

    public boolean isEntry() {
        return this == LONG_ENTRY || this == SHORT_ENTRY;
    }

    public boolean isExit() {
        return !isEntry();
    }

    public Side entrySide() {
        return this == LONG_ENTRY ? Side.LONG : Side.SHORT;
    }

}
