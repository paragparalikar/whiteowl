package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteTransactionType;
import com.whiteowl.core.order.model.OrderSide;

final class KiteOrderSideMapper {

    public OrderSide toOrderSide(KiteTransactionType transactionType) {
        if (transactionType == null) return null;
        return switch (transactionType) {
            case BUY -> OrderSide.BUY;
            case SELL -> OrderSide.SELL;
        };
    }

    public KiteTransactionType toKiteTransactionType(OrderSide side) {
        if (side == null) return null;
        return switch (side) {
            case BUY -> KiteTransactionType.BUY;
            case SELL -> KiteTransactionType.SELL;
        };
    }

}
