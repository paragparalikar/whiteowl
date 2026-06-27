package com.whiteowl.core.backtest.engine;

import com.whiteowl.core.backtest.model.TradeRecord;
import com.whiteowl.core.order.model.OrderSide;

final class PositionTracker {

    private static final float HUNDRED = 100f;

    private OrderSide side;
    private int entryBarIndex;
    private float entryPrice;
    private long entryTimestamp;
    private int quantity;
    private boolean hasPosition;

    boolean hasOpenPosition() {
        return hasPosition;
    }

    OrderSide getSide() {
        return side;
    }

    void openPosition(OrderSide side, int barIndex, float price, long timestamp, int qty) {
        this.side = side;
        this.entryBarIndex = barIndex;
        this.entryPrice = price;
        this.entryTimestamp = timestamp;
        this.quantity = qty;
        this.hasPosition = true;
    }

    TradeRecord closePosition(String scripId, int barIndex, float price, long timestamp,
                              float costPercent) {
        float grossPnl = side.getMultiplier() * (price - entryPrice) * quantity;
        float entryCost = entryPrice * quantity * costPercent / HUNDRED;
        float exitCost = price * quantity * costPercent / HUNDRED;
        float netPnl = grossPnl - entryCost - exitCost;
        float invested = entryPrice * quantity;
        float netPnlPercent = invested > 0 ? netPnl / invested * HUNDRED : 0f;
        hasPosition = false;
        return TradeRecord.builder()
                .scripId(scripId)
                .side(side)
                .entryBarIndex(entryBarIndex)
                .entryPrice(entryPrice)
                .entryTimestamp(entryTimestamp)
                .exitBarIndex(barIndex)
                .exitPrice(price)
                .exitTimestamp(timestamp)
                .quantity(quantity)
                .grossPnl(grossPnl)
                .netPnl(netPnl)
                .netPnlPercent(netPnlPercent)
                .build();
    }

}
