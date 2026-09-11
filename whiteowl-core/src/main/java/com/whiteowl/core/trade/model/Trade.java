package com.whiteowl.core.trade.model;

import com.whiteowl.core.order.model.OrderSide;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public final class Trade {

    private final String id;
    private String portfolioId;
    private String scripId;
    private String strategyId;
    private OrderSide side;
    private TradeStatus status;
    private long entryTimestamp;
    private int entryQuantity;
    private float entryPrice;
    private long exitTimestamp;
    private int exitQuantity;
    private float exitPrice;

    public final float getRealizedPnl() {
        return side.getMultiplier() * (exitPrice * exitQuantity - entryPrice * entryQuantity);
    }

    public final float getRealizedPnlPercent() {
        float invested = entryPrice * entryQuantity;
        if (invested == 0f) {
            return 0f;
        }
        return side.getMultiplier() * (exitPrice * exitQuantity - invested) * 100f / invested;
    }

    public final boolean isOpen() {
        return entryQuantity != exitQuantity;
    }

    public final long getDuration() {
        return exitTimestamp - entryTimestamp;
    }

}
