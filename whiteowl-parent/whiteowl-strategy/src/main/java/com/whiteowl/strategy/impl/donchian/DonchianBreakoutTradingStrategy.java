package com.whiteowl.strategy.impl.donchian;

import static org.ta4j.core.Trade.TradeType.BUY;

import java.util.List;
import java.util.Optional;

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
import com.whiteowl.strategy.TradingStrategy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DonchianBreakoutTradingStrategy implements TradingStrategy {
	
	@NonNull private final Scrip scrip;
	@NonNull private final List<Bar> bars;
	@NonNull private final DonchianBreakoutTradingStrategyConfig config;
	
	@Override
	public Optional<Position> enter() {
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
				if(high > close) return Optional.empty();
				if(high > previousClose) previousCloseIsNotHighest = true;
			}
			return previousCloseIsNotHighest ? 
					Optional.of(createNewPosition(scrip, bars, TradeType.BUY, config)): 
					Optional.empty(); 
		} else if(close < previousClose) { // check for short position
			boolean previousCloseIsNotLowest = false;
			for(int index = previousBarIndex; index < previousBarIndex - lowerBandLength; index--) {
				final double low = bars.get(index).getLowPrice().doubleValue();
				if(low < close) return Optional.empty();
				if(low < previousClose) previousCloseIsNotLowest = true;
			}
			return previousCloseIsNotLowest ? 
					Optional.of(createNewPosition(scrip, bars, TradeType.SELL, config)) : 
					Optional.empty(); 
		} else {
			return Optional.empty();
		}
	}
	
	@Override
	public boolean manage(@NonNull Position position) {
		return manage(position, bars.get(bars.size() - 1).getClosePrice().doubleValue());
	}
	
	@Override
	public boolean manage(@NonNull Position position, @NonNull Quote quote) {
		return manage(position, quote.getLastPrice());
	}
	
	private boolean manage(Position position, double lastPrice) {
		final double entryPrice = position.getAverageEntryPrice();
		final double stopLossPercentage = config.getInitialPriceStopPercentage();
		final TradeType tradeType = position.getEntryTrades().iterator().next().getType();
		if(BUY.equals(tradeType)) {
			final double stopLossPrice = entryPrice - (entryPrice * stopLossPercentage / 100);
			if(lastPrice <= stopLossPrice) {
				closePosition(position);
				return true;
			}
		} else {
			final double stopLossPrice = entryPrice + (entryPrice * stopLossPercentage / 100);
			if(lastPrice >= stopLossPrice) {
				closePosition(position);
				return true;
			}
		}
		return false;
	}
	
	private void closePosition(Position position) {
		position.getEntryTrades().stream()
			.map(this::createExitTrade)
			.forEach(position.getExitTrades()::add);
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
		final Trade trade = createEntryTrade(scrip, bars, tradeType, config);
		position.getEntryTrades().add(trade);
		return position;
	}
	
	private Trade createEntryTrade(
			@NonNull final Scrip scrip,
			@NonNull final List<Bar> bars,
			@NonNull final TradeType tradeType,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final Trade entryTrade = new Trade();
		entryTrade.setScrip(scrip);
		entryTrade.setQuantity(1); // TODO calculate quantity
		entryTrade.setType(tradeType);
		entryTrade.setPrice(bars.get(bars.size() - 1).getClosePrice().doubleValue());
		entryTrade.setProduct(TradeProduct.NRML);
		entryTrade.setStatus(TradeStatus.NEW);
		entryTrade.setLimitType(TradeLimitType.LIMIT);
		entryTrade.setValidity(TradeValidity.DAY);
		entryTrade.setVariety(TradeVariety.REGULAR);
		return entryTrade;
	}
	
	private Trade createExitTrade(@NonNull final Trade entryTrade) {
		final Trade exitTrade = entryTrade.complement();
		exitTrade.setLimitType(TradeLimitType.MARKET);
		return exitTrade;
	}

}
