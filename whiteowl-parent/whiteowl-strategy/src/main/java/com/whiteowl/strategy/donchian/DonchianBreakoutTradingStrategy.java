package com.whiteowl.strategy.donchian;

import static org.ta4j.core.Trade.TradeType.BUY;

import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;

import lombok.NonNull;

public class DonchianBreakoutTradingStrategy {
	
	boolean closePosition(DonchianBreakoutTradingStrategyConfig config, Position position, Quote quote) {
		final double lastPrice = quote.getLastPrice();
		final double entryPrice = position.getAverageEntryPrice();
		final double stopLossPercentage = config.getInitialPriceStopPercentage();
		final TradeType tradeType = position.getEntryTrades().iterator().next().getType();
		if(BUY.equals(tradeType)) {
			final double stopLossPrice = entryPrice - (entryPrice * stopLossPercentage / 100);
			
		}
		
		return false;
	}

	Position openPosition(
			@NonNull final Scrip scrip,
			@NonNull final List<Bar> bars,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final int upperBandLength = config.getUpperBandLength();
		final int lowerBandLength = config.getLowerBandLength();
		final int maxLength = Math.max(upperBandLength, lowerBandLength);
		if(bars.size() < maxLength + 2) return null; 
		final int lastIndex = bars.size() - 1;
		final int previousBarIndex = lastIndex - 1;
		final double close = bars.get(lastIndex).getClosePrice().doubleValue();
		final double previousClose = bars.get(previousBarIndex).getClosePrice().doubleValue();
		
		if(close > previousClose) { // check for long position
			boolean previousCloseIsNotHighest = false;
			for(int index = previousBarIndex; index < previousBarIndex - upperBandLength; index--) {
				final double high = bars.get(index).getHighPrice().doubleValue();
				if(high > close) return null;
				if(high > previousClose) previousCloseIsNotHighest = true;
			}
			return previousCloseIsNotHighest ? createNewPosition(scrip, bars, TradeType.BUY, config) : null; 
		} else if(close < previousClose) { // check for short position
			boolean previousCloseIsNotLowest = false;
			for(int index = previousBarIndex; index < previousBarIndex - lowerBandLength; index--) {
				final double low = bars.get(index).getLowPrice().doubleValue();
				if(low < close) return null;
				if(low < previousClose) previousCloseIsNotLowest = true;
			}
			return previousCloseIsNotLowest ? createNewPosition(scrip, bars, TradeType.SELL, config) : null; 
		} else {
			return null;
		}
	}
	
	private Position createNewPosition(
			@NonNull final Scrip scrip,
			@NonNull final List<Bar> bars,
			@NonNull final TradeType tradeType,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setStatus(PositionStatus.NEW);
		position.setTradingStrategyConfigId(config.getId());
		final Trade trade = createNewTrade(scrip, bars, tradeType, config);
		position.getEntryTrades().add(trade);
		return position;
	}
	
	private Trade createNewTrade(
			@NonNull final Scrip scrip,
			@NonNull final List<Bar> bars,
			@NonNull final TradeType tradeType,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final Trade trade = new Trade();
		trade.setScrip(scrip);
		trade.setQuantity(1); // TODO calculate quantity
		trade.setType(tradeType);
		trade.setPrice(bars.get(bars.size() - 1).getClosePrice().doubleValue());
		trade.setProduct(TradeProduct.NRML);
		trade.setStatus(TradeStatus.NEW);
		trade.setLimitType(TradeLimitType.LIMIT);
		trade.setValidity(TradeValidity.DAY);
		trade.setVariety(TradeVariety.REGULAR);
		return trade;
	}

}
