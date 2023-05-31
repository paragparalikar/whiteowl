package com.whiteowl.core.analysis.backtester.mock.broker;

import java.util.EnumMap;
import java.util.Map;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;

public class CompositeTradeExecutor implements TradeExecutor {

	private final Map<TradeLimitType, TradeExecutor> delegates = new EnumMap<>(TradeLimitType.class);
	
	public CompositeTradeExecutor() {
		delegates.put(TradeLimitType.MARKET, new MarketTradeExecutor());
		delegates.put(TradeLimitType.LIMIT, new LimitTradeExecutor());
		delegates.put(TradeLimitType.SL, new StoplossLimitTradeExecutor());
		delegates.put(TradeLimitType.SLM, new StoplossMarketTradeExecutor());
	}

	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		return delegates.get(trade.getLimitType()).execute(trade, quote, slippagePercentage);
	}

}
