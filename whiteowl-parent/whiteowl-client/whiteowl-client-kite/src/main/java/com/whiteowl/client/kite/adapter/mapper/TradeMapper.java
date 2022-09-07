package com.whiteowl.client.kite.adapter.mapper;

import java.time.ZonedDateTime;

import com.whiteowl.client.kite.model.Order;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.util.Strings;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TradeMapper {

	private final KiteMapper kiteMapper;
	
	public Order toOrder(Trade trade) {
		if(null == trade) return null;
		final Order order = new Order();
		order.setTradingsymbol(trade.getScripCode());
		order.setExchange(kiteMapper.toKiteExchange(trade.getExchange()));
		order.setPrice(trade.getPrice());
		order.setTriggerPrice(trade.getTriggerPrice());
		order.setQuantity(trade.getQuantity());
		order.setDisclosedQuantity(trade.getDisclosedQuantity());
		order.setVariety(kiteMapper.toOrderVariety(trade.getVariety()));
		order.setTransactionType(kiteMapper.toTransactionType(trade.getType()));
		order.setOrderType(kiteMapper.toOrderType(trade.getLimitType()));
		order.setProduct(kiteMapper.toProduct(trade.getProduct()));
		order.setValidity(kiteMapper.toOrderValidity(trade.getValidity()));
		return order;
	}
	
	public Trade toTrade(Order order) {
		if(null == order) return null;
		Strings.requireText(order.getOrderId(), "Order coming from kite can not have null id");
		final Trade trade = new Trade();
		trade.setScripCode(order.getTradingsymbol());
		trade.setBrokerTradeId(order.getOrderId());
		trade.setExchangeTradeId(order.getExchangeOrderId());
		//trade.setTradingStrategyConfigId(""); // ??
		trade.setStatusMessage(order.getStatusMessage());
		trade.setStopLossPrice(order.getStoploss());
		trade.setPrice(order.getPrice());
		trade.setTriggerPrice(order.getTriggerPrice());
		trade.setAveragePrice(order.getAveragePrice());
		trade.setQuantity(order.getQuantity());
		trade.setPendingQuantity(order.getPendingQuantity());
		trade.setFilledQuantity(order.getFilledQuantity());
		trade.setDisclosedQuantity(order.getDisclosedQuantity());
		trade.setType(kiteMapper.toTradeType(order.getTransactionType()));
		trade.setLimitType(kiteMapper.toTradeLimitType(order.getOrderType()));
		trade.setVariety(kiteMapper.toTradeVariety(order.getVariety()));
		trade.setProduct(kiteMapper.toTradeProduct(order.getProduct()));;
		trade.setStatus(kiteMapper.toTradeStatus(order.getStatus()));
		trade.setExchange(kiteMapper.toExchange(order.getExchange()));
		trade.setTimestamp(ZonedDateTime.parse(order.getOrderTimestamp()));
		trade.setExchangeTimestamp(ZonedDateTime.parse(order.getExhangeTimestamp()));
		return trade;
	}
	
}
