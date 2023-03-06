package com.whiteowl.core.strategy.impl;

import java.util.Arrays;
import java.util.List;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.TradingStrategyContext;
import com.whiteowl.core.trade.Trade;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractTradingStrategy<T extends TradingStrategyConfig> {

	private final T config;
	private final Scrip scrip;
	private final Timeframe timeframe;
	private final BarSeries barSeries;
	private final TradingStrategyContext context;
	
	public AbstractTradingStrategy(T config, TradingStrategyContext context) {
		this.config= config;
		this.context = context;
		this.timeframe = config.getTimeframe();
		this.scrip = context.getScrip(config.getScripCode());
		this.barSeries = context.getBarSeries(config.getScripCode(), timeframe);
		context.subscribe(scrip, timeframe, this::onBar);
		context.subscribe(Arrays.asList(scrip), QuoteMode.FULL, this::onQuote);
	}
	
	protected abstract Rule createEntryRule(BarSeries barSeries, T config);
	
	protected void onBar() {
		final List<Position> positions = context.getOpenPositions(config.getId());
		if(positions.isEmpty()) enter();
	}
	
	protected void onQuote(Quote quote) {
		final List<Position> positions = context.getOpenPositions(config.getId());
		positions.forEach(position -> manage(quote, position));
	}
	
	protected abstract boolean shouldEnter();
	
	protected abstract Trade createEntryTrade();
	
	protected Position createNewPosition() {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setTradingStrategyConfigId(config.getId());
		return position;
	}
	
	protected void enter() {
		if(shouldEnter()) {
			final Trade entryTrade = createEntryTrade();
			final Position position = createNewPosition();
			position.getEntryTrades().add(entryTrade);
			context.save(position);
		}
	}
	
	protected void exit(Quote quote, Position position) {
		position.getEntryTrades().stream()
			.map(entryTrade -> createExitTrade(quote, position, entryTrade))
			.forEach(position.getExitTrades()::add);
		context.save(position);
	}
	
	protected Trade createExitTrade(Quote quote, Position position, Trade entryTrade) {
		return entryTrade.complement();
	}
	
	protected void manage(Quote quote, Position position) {
		final Double lastPrice = quote.getLastPrice();
		final Double targetPrice = position.getTargetPrice();
		if(null != targetPrice) {
			if(Positions.isLong(position)) {
				if(lastPrice >= targetPrice) {
					exit(quote, position);
				}
			} else if(Positions.isShort(position)) {
				if(lastPrice <= targetPrice) {
					exit(quote, position);
				}
			}
		}
		final Double stopLossPrice = position.getStopLossPrice();
		if(null != stopLossPrice) {
			if(Positions.isLong(position)) {
				if(lastPrice <= stopLossPrice) {
					exit(quote, position);
				}
			} else if(Positions.isShort(position)) {
				if(lastPrice >= stopLossPrice) {
					exit(quote, position);
				}
			}
		}
	}
	
}
