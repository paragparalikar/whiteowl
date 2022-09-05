package com.whiteowl.client.kite.adapter.mapper;

import java.time.LocalDateTime;

import com.whiteowl.client.kite.model.Order;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeService;
import com.whiteowl.core.util.Strings;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TradeMapper {

	private final KiteMapper kiteMapper;
	private final TradeService tradeService;
	
	public Order toOrder(Trade trade) {
		if(null == trade) return null;
		final Order order = new Order();
		
		order.setTag(String.valueOf(trade.getId()));
		order.setTradingsymbol(trade.getScrip().getCode());
		order.setExchange(kiteMapper.toKiteExchange(trade.getScrip().getExchange()));
		
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
		final Trade trade = tradeService.findByBrokerTradeId(order.getOrderId()).orElse(null);
		if(null != trade) {
			trade.setStatus(kiteMapper.toTradeStatus(order.getStatus()));
			trade.setTimestamp(LocalDateTime.parse(order.getOrderTimestamp()));
			trade.setExchangeTimestamp(LocalDateTime.parse(order.getExhangeTimestamp()));
			trade.setAveragePrice(order.getAveragePrice());
			trade.setStatusMessage(order.getStatusMessage());
			trade.setFilledQuantity(order.getFilledQuantity());
			trade.setPendingQuantity(order.getPendingQuantity());
		}
		return trade;
	}
	
}
