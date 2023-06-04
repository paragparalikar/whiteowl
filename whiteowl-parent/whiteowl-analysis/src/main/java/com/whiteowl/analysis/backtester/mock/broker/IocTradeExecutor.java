package com.whiteowl.analysis.backtester.mock.broker;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.util.Trades;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IocTradeExecutor implements TradeExecutor {

	private final TradeExecutor tradeExecutor;
	
	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		if(!tradeExecutor.execute(trade, quote, slippagePercentage)) {
			trade.setStatus(TradeStatus.CANCELLED);
			Trades.setTimestamps(trade, quote.getTimestamp());
		}
		return true;
	}

}
