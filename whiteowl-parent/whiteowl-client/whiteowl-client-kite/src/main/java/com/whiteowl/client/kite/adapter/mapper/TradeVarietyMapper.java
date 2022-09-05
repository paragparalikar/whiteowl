package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.OrderVariety;
import com.whiteowl.core.trade.TradeVariety;

public class TradeVarietyMapper {

	public TradeVariety toTradeVariety(OrderVariety orderVariety) {
		if(null == orderVariety) return null;
		switch(orderVariety) {
		case AMO: return TradeVariety.AMO;
		case BO: return TradeVariety.BO;
		case CO: return TradeVariety.CO;
		case REGULAR: return TradeVariety.REGULAR;
		default: throw new IllegalArgumentException(String.format("OrderVariety %s is not supported", orderVariety));
		}
	}
	
	public OrderVariety toOrderVariety(TradeVariety tradeVariety) {
		if(null == tradeVariety) return null;
		switch(tradeVariety) {
		case AMO:return OrderVariety.AMO;
		case BO: return OrderVariety.BO;
		case CO:return OrderVariety.CO;
		case REGULAR:return OrderVariety.REGULAR;
		default: throw new IllegalArgumentException(String.format("TradeVariety %s is not supported", tradeVariety.name()));
		}
	}
	
}
