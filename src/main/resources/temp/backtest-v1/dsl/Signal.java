package com.whiteowl.core.backtest.dsl;

public record Signal(SignalType type, int barIndex, FillTiming fillTiming, FillPrice fillPrice) {

    private static final FillTiming DEFAULT_TIMING = FillTiming.NEXT_BAR;
    private static final FillPrice DEFAULT_PRICE = FillPrice.OPEN;

    public Signal(SignalType type, int barIndex) {
        this(type, barIndex, DEFAULT_TIMING, DEFAULT_PRICE);
    }

}
