package com.whiteowl.core.analysis.backtester.mock.broker;

import static com.whiteowl.core.trade.TradeStatus.COMPLETE;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.util.Trades;

public class MarketTradeExecutor implements TradeExecutor {

	@Override
	public boolean execute(Trade trade, Quote quote, double slippagePercentage) {
		final double price = quote.getLastPrice();
		if(TradeType.BUY.equals(trade.getType())) {
			final double buyPrice = price * (100 + slippagePercentage) / 100;
			trade.setAveragePrice(buyPrice);
		} else {
			final double sellPrice = price * (100 - slippagePercentage) / 100;
			trade.setAveragePrice(sellPrice);
		}
		trade.setStatus(COMPLETE);
		trade.setFilledQuantity(trade.getQuantity());
		Trades.setTimestamps(trade, quote.getLastTradeTime());
		return true;
	}

}
