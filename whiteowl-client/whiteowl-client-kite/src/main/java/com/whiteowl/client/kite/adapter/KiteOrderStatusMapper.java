package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteOrderStatus;
import com.whiteowl.core.order.model.OrderStatus;

final class KiteOrderStatusMapper {

    public OrderStatus toOrderStatus(KiteOrderStatus kiteStatus) {
        if (kiteStatus == null) return null;
        return switch (kiteStatus) {
            case PUT_ORDER_REQUEST_RECEIVED -> OrderStatus.DISPATCHED;
            case VALIDATION_PENDING, OPEN_PENDING, TRIGGER_PENDING, AMO_REQ_RECEIVED -> OrderStatus.PENDING;
            case OPEN -> OrderStatus.OPEN;
            case MODIFY_VALIDATION_PENDING, MODIFY_PENDING -> OrderStatus.UPDATE_PENDING;
            case CANCEL_PENDING -> OrderStatus.CANCEL_PENDING;
            case CANCELLED -> OrderStatus.CANCELLED;
            case REJECTED -> OrderStatus.REJECTED;
            case COMPLETE -> OrderStatus.COMPLETE;
        };
    }

}
