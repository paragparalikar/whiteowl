package com.whiteowl.strategy.impl.trendfollowing;

import java.util.Optional;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

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

public class TrendFollowingTradingStrategy implements TradingStrategy {

	private final Scrip scrip;
	private final BarSeries barSeries;
	private final Rule downCrossRule, upCrossRule;
	private final TrendFollowingTradingStrategyConfig config;
	
	public TrendFollowingTradingStrategy(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull final TrendFollowingTradingStrategyConfig config) {
		this.scrip = scrip;
		this.config = config;
		this.barSeries = barSeries;
		final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
		final Indicator<Num> smaIndicator = new SMAIndicator(closePriceIndicator, config.getSmaBarCount());
		upCrossRule = new CrossedUpIndicatorRule(closePriceIndicator, smaIndicator);
		downCrossRule = new CrossedDownIndicatorRule(closePriceIndicator, smaIndicator);
	}
	
	private Position createNewPosition(final double price, final TradeType tradeType) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setTradingStrategyConfigId(config.getId());
		final Trade trade = createEntryTrade(price, tradeType);
		position.getEntryTrades().add(trade);
		return position;
	}
	
	private Trade createEntryTrade(final double price, final TradeType tradeType) {
		final Trade entryTrade = new Trade();
		entryTrade.setScrip(scrip);
		entryTrade.setQuantity(1); 
		entryTrade.setType(tradeType);
		entryTrade.setPrice(price);
		entryTrade.setProduct(TradeProduct.MIS);
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
	
	@Override
	public Optional<Position> enter() {
		if(upCrossRule.isSatisfied(barSeries.getEndIndex())) {
			final TradeType tradeType = TradeType.BUY;
			final double price = barSeries.getLastBar().getClosePrice().doubleValue();
			return Optional.of(createNewPosition(price, tradeType));
		}
		return Optional.empty();
	}
	
	@Override
	public boolean quantify(@NonNull Position position, final double amount) {
		if(!PositionStatus.NEW.equals(position.getStatus())) throw new IllegalStateException("Position must be in NEW status");
		if(!position.getExitTrades().isEmpty()) throw new IllegalStateException("There should be no exit trades at this point");
		if(position.getEntryTrades().isEmpty()) throw new IllegalStateException("There are no entry trade to quantify");
		if(1 < position.getEntryTrades().size()) throw new IllegalStateException("This strategy does not support multiple entry trades");
		final Trade entryTrade = position.getEntryTrades().iterator().next();
		if(!TradeStatus.NEW.equals(entryTrade.getStatus())) throw new IllegalStateException("Entry trade must be in NEW state to quantify");
		final double price = entryTrade.getPrice();
		if(amount < price) {
			return false;
		} else {
			entryTrade.setQuantity((int) (amount / price));
			return true;
		}
	}

	@Override
	public boolean manage(@NonNull Position position) {
		if(!position.getStatus().isTerminal()) {
			final Trade entryTrade = position.getEntryTrades().iterator().next();
			if(TradeStatus.COMPLETE.equals(entryTrade.getStatus()) && position.getExitTrades().isEmpty()) {
				final Rule exitRule = TradeType.BUY.equals(entryTrade.getType()) ? downCrossRule : upCrossRule;
				if(exitRule.isSatisfied(barSeries.getEndIndex())) {
					return position.getExitTrades().add(createExitTrade(entryTrade));
				}
			}
		}
		return false;
	}

	@Override
	public boolean manage(@NonNull Position position, @NonNull Quote quote) {
		return false;
	}

}
