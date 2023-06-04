package com.whiteowl.analysis.backtester.mock.broker;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;

public class MockTradeExecutor implements TradeExecutor {

	private final TradeExecutor delegate = 
			new ValidityAwareTradeExecutor(new LimitTypeAwareTradeExecutor());

	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		return delegate.execute(trade, quote, slippagePercentage);
	}

}
