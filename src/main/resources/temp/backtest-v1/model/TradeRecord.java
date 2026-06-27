package com.whiteowl.core.backtest.model;

import com.whiteowl.core.order.model.OrderSide;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class TradeRecord {

    private final String scripId;
    private final OrderSide side;
    private final int entryBarIndex;
    private final float entryPrice;
    private final long entryTimestamp;
    private final int exitBarIndex;
    private final float exitPrice;
    private final long exitTimestamp;
    private final int quantity;
    private final float grossPnl;
    private final float netPnl;
    private final float netPnlPercent;

}
