package com.whiteowl.core.backtest.v2.feature;

import com.whiteowl.core.backtest.v2.engine.OpenPosition;
import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.BarsArrays;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class TradeLifecycleEvent {

    private final TradeLifecyclePhase phase;
    private final String scripId;
    private final int barIndex;
    private final BarsArrays arrays;
    private final Side side;
    private final int positionId;
    private final float entryPrice;
    private final long entryTimestamp;
    private final int entryBarIndex;
    private final int quantity;
    private final int barsInTrade;
    private final float unrealizedPnl;
    private final TradeRecord tradeRecord;

    public float currentOpen() {
        return arrays.open()[barIndex];
    }

    public float currentHigh() {
        return arrays.high()[barIndex];
    }

    public float currentLow() {
        return arrays.low()[barIndex];
    }

    public float currentClose() {
        return arrays.close()[barIndex];
    }

    public long currentVolume() {
        return arrays.volume()[barIndex];
    }

    public long currentTimestamp() {
        return arrays.timestamp()[barIndex];
    }

}
