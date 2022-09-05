package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.OrderValidity;
import com.whiteowl.core.trade.TradeValidity;

public class TradeValidityMapper {

	public TradeValidity toTradeValidity(OrderValidity orderValidity) {
		if(null == orderValidity) return null;
		switch(orderValidity) {
		case DAY:return TradeValidity.DAY;
		case IOC:return TradeValidity.IOC;
		default:throw new IllegalArgumentException(String.format("OrderValidity %s is not supported", orderValidity.name()));
		}
	}
	
	public OrderValidity toOrderValidity(TradeValidity tradeValidity) {
		if(null == tradeValidity) return null;
		switch(tradeValidity) {
		case DAY:return OrderValidity.DAY;
		case IOC:return OrderValidity.IOC;
		default:throw new IllegalArgumentException(String.format("TradeValidity %s is not supported", tradeValidity.name()));
		}
	}
	
}
