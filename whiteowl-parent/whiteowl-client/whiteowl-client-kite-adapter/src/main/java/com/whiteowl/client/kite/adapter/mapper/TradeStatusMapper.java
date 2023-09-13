package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.OrderStatus;
import com.whiteowl.core.trade.TradeStatus;

public class TradeStatusMapper {

	public TradeStatus toTradeStatus(OrderStatus orderStatus) {
		if(null == orderStatus) return null;
		switch(orderStatus) {
		case VALIDATION_PENDING:
		case TRIGGER_PENDING:
		case AMO_REQ_RECEIVED:
		case PUT_ORDER_REQUEST_RECEIVED:
		case OPEN_PENDING: return TradeStatus.PENDING;
		case MODIFY_PENDING:
		case MODIFY_VALIDATION_PENDING:
		case CANCEL_PENDING:
		case OPEN: return TradeStatus.OPEN;
		case CANCELLED: return TradeStatus.CANCELLED;
		case COMPLETE: return TradeStatus.COMPLETE;
		case REJECTED: return TradeStatus.REJECTED;
		default:throw new IllegalArgumentException(String.format("OrderStatus %s is not supported", orderStatus.name()));
		}
	}
	
}
