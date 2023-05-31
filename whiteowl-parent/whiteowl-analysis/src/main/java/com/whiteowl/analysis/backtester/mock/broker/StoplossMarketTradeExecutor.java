package com.whiteowl.analysis.backtester.mock.broker;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;

public class StoplossMarketTradeExecutor extends MarketTradeExecutor {

	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		if(TradeType.BUY.equals(trade.getType()) && quote.getLastPrice() >= trade.getPrice()) {
			return super.execute(trade, quote, slippagePercentage);
		} else if(TradeType.SELL.equals(trade.getType()) && quote.getLastPrice() <= trade.getPrice()) {
			return super.execute(trade, quote, slippagePercentage);
		}
		return false;
	}
	
}
