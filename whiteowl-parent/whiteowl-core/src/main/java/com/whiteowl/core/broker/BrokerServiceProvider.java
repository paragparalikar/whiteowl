package com.whiteowl.core.broker;

import java.util.List;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeProduct;

public interface BrokerServiceProvider {
	
	Broker getBrokerType();

	List<Trade> findAllTrades(Portfolio portfolio);
	
	void create(Trade trade, Portfolio portfolio);

	void update(Trade trade, Portfolio portfolio);

	void cancel(Trade trade, Portfolio portfolio);
	
	int getAvailableQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio);
	
}
