package com.whiteowl.core.backtest.v2.model;

import com.whiteowl.core.backtest.v2.engine.Side;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class TradeRecord {

    private final int positionId;
    private final String scripId;
    private final Side side;
    private final float entryPrice;
    private final float exitPrice;
    private final long entryTimestamp;
    private final long exitTimestamp;
    private final int quantity;
    private final float grossPnl;
    private final float totalCost;
    private final float netPnl;
    private final float netPnlPercent;
    private final int entryBarIndex;
    private final int exitBarIndex;
    private final int holdingBars;

}
