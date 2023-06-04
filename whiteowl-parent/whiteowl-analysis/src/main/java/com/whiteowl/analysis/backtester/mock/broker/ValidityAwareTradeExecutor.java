package com.whiteowl.analysis.backtester.mock.broker;

import java.util.EnumMap;
import java.util.Map;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeValidity;

public class ValidityAwareTradeExecutor implements TradeExecutor {

	private final Map<TradeValidity, TradeExecutor> delegates = new EnumMap<>(TradeValidity.class);
	
	public ValidityAwareTradeExecutor(TradeExecutor delegate) {
		delegates.put(TradeValidity.DAY, new DayTradeExecutor(delegate));
		delegates.put(TradeValidity.IOC, new IocTradeExecutor(delegate));
	}
	
	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		return delegates.get(trade.getValidity()).execute(trade, quote, slippagePercentage);
	}

	

}
