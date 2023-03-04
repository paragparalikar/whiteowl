package com.whiteowl.core.strategy.impl.trendfollowing;

import java.util.Collections;
import java.util.List;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

public class SMATradingStrategy {

	private final Scrip scrip;
	private final Timeframe timeframe;
	private final BarSeries barSeries;
	private final Indicator<Num> smaIndicator;
	private final TradingStrategyContext context;
	private final SMATradingStrategyConfig config;
	
	
	public SMATradingStrategy(SMATradingStrategyConfig config, TradingStrategyContext context) {
		this.config = config;
		this.context = context;
		this.timeframe = config.getTimeframe();
		this.scrip = context.getScrip(config.getScripCode());
		this.barSeries = context.getBarSeries(config.getScripCode(), timeframe);
		final Indicator<Num> closePriceIndicator = new ClosePriceIndicator(barSeries);
		this.smaIndicator = new SMAIndicator(closePriceIndicator, config.getBarCount());
		context.subscribe(scrip, timeframe, this::onBar);
		context.subscribe(Collections.singletonList(scrip), QuoteMode.FULL, this::onQuote);
	}
	
	private void onQuote(Quote quote) {
		final List<Position> positions = context.getOpenPositions(
						config.getId(), PositionStatus.CLOSED);
		
	}
	
	private void onBar() {
		
	}
	
	

}
