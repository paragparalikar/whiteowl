package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.OrderType;
import com.whiteowl.core.trade.TradeLimitType;

public class TradeLimitTypeMapper {

	public TradeLimitType toTradeLimitType(OrderType orderType) {
		if(null == orderType) return null;
		switch(orderType){
		case LIMIT:return TradeLimitType.LIMIT;
		case MARKET:return TradeLimitType.MARKET;
		case SL:return TradeLimitType.SL;
		case SLM:return TradeLimitType.SLM;
		default:throw new IllegalArgumentException(String.format("OrderType %s is not supported", orderType.name()));
		}
	}
	
	public OrderType toOrderType(TradeLimitType tradeLimitType) {
		if(null == tradeLimitType) return null;
		switch(tradeLimitType){
		case LIMIT:return OrderType.LIMIT;
		case MARKET:return OrderType.MARKET;
		case SL:return OrderType.SL;
		case SLM:return OrderType.SLM;
		default:throw new IllegalArgumentException(String.format("TradeLimitType %s is not supported", tradeLimitType.name()));
		}
	}
	
}
