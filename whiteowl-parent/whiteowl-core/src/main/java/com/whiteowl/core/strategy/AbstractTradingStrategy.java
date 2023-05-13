package com.whiteowl.core.strategy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
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
public abstract class AbstractTradingStrategy<T extends TradingStrategyConfig> implements TradingStrategy {

	private final T config;
	private final Scrip scrip;
	private final Timeframe timeframe;
	private final BarSeries barSeries;
	private final TradingStrategyContext context;
	private final Consumer<Bar> barListener = this::onBar;
	private final Consumer<Quote> quoteListener = this::onQuote;
	
	public AbstractTradingStrategy(Scrip scrip, Timeframe timeframe, 
			T config, TradingStrategyContext context) {
		this.scrip = scrip;
		this.config= config;
		this.context = context;
		this.timeframe = timeframe;
		this.barSeries = context.getBarSeries(scrip.getCode(), timeframe);
		context.subscribe(scrip, timeframe, barListener);
		context.subscribe(Arrays.asList(scrip), QuoteMode.FULL, quoteListener);
	}
	
	private void onBar(Bar bar) {
		final boolean hasBars = config.getMinBarCount() <= barSeries.getBarCount();
		final boolean hasPositions = context.existsByScripAndStatusNot(scrip, PositionStatus.CLOSED);
		if(hasBars && !hasPositions && shouldEnter()) {
			final Collection<Trade> entryTrades = createEntryTrades();
			final Position position = createNewPosition(bar.getEndTime().toLocalDateTime());
			position.getEntryTrades().addAll(entryTrades);
			context.save(position);
		}
	}
	
	private void onQuote(Quote quote) {
		final List<Position> positions = context.getOpenPositions(config.getId());
		if(null != positions && !positions.isEmpty()) {
			for(int index = 0; index < positions.size(); index++) {
				final Position position = positions.get(index);
				if(shouldExit(quote, position)) {
					position.getEntryTrades().stream()
						.map(entryTrade -> createExitTrade(quote, position, entryTrade))
						.forEach(position.getExitTrades()::add);
					context.save(position);
				}
			}
		}
	}
	
	protected abstract boolean shouldEnter();
	
	protected abstract Collection<Trade> createEntryTrades();
	
	protected Position createNewPosition(LocalDateTime timestamp) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setTradingStrategyConfigId(config.getId());
		position.setCreatedDate(timestamp);
		return position;
	}
	
	protected boolean shouldExit(Quote quote, Position position) {
		final Double lastPrice = quote.getLastPrice();
		final Double targetPrice = position.getTargetPrice();
		if(null != targetPrice) {
			if(Positions.isLong(position)) {
				if(lastPrice >= targetPrice) {
					return true;
				}
			} else if(Positions.isShort(position)) {
				if(lastPrice <= targetPrice) {
					return true;
				}
			}
		}
		final Double stopLossPrice = position.getStopLossPrice();
		if(null != stopLossPrice) {
			if(Positions.isLong(position)) {
				if(lastPrice <= stopLossPrice) {
					return true;
				}
			} else if(Positions.isShort(position)) {
				if(lastPrice >= stopLossPrice) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected Trade createExitTrade(Quote quote, Position position, Trade entryTrade) {
		return entryTrade.complement();
	}
	
	@Override
	public void close() throws Exception {
		context.unsubscribeBarListener(barListener);
		context.unsubscribeQuoteListener(quoteListener);
	}
	
}
