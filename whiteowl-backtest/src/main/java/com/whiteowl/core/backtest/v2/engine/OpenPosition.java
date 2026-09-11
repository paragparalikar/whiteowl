package com.whiteowl.core.backtest.v2.engine;

import lombok.Getter;

@Getter
public final class OpenPosition {

    private final int id;
    private final Side side;
    private final int quantity;
    private final float entryPrice;
    private final long entryTimestamp;
    private final int entryBarIndex;

    public OpenPosition(int id, Side side, int quantity, float entryPrice,
                        long entryTimestamp, int entryBarIndex) {
        this.id = id;
        this.side = side;
        this.quantity = quantity;
        this.entryPrice = entryPrice;
        this.entryTimestamp = entryTimestamp;
        this.entryBarIndex = entryBarIndex;
    }

    public float unrealizedPnl(float currentPrice) {
        float diff = currentPrice - entryPrice;
        if (side == Side.SHORT) {
            diff = -diff;
        }
        return diff * quantity;
    }

}
