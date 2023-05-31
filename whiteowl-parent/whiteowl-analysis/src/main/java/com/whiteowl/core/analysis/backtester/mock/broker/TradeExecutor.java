package com.whiteowl.core.analysis.backtester.mock.broker;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;

public interface TradeExecutor {
	
	boolean execute(Trade trade, Quote quote, double slippagePercentage);

}
