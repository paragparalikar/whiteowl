package com.whiteowl.analysis.backtester.mock.broker;

import java.time.LocalDateTime;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.util.Trades;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DayTradeExecutor implements TradeExecutor {
	
	private final TradeExecutor delegate;
	private volatile LocalDateTime timestamp;

	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		if(null != timestamp && quote.getTimestamp().isBefore(timestamp)) {
			timestamp = quote.getTimestamp(); // Reset, as we have started iterating over bar series again from beginning
		}
		if(null != timestamp && quote.getTimestamp().toLocalDate().isAfter(timestamp.toLocalDate())) {
			timestamp = quote.getTimestamp();
			Trades.setTimestamps(trade, timestamp);
			trade.setStatus(TradeStatus.CANCELLED);
			return true;
		}
		timestamp = quote.getTimestamp();
		return delegate.execute(trade, quote, slippagePercentage);
	}

}
