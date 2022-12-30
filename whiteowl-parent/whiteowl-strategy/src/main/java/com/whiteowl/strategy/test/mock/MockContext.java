package com.whiteowl.strategy.test.mock;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.TradingStrategy;
import com.whiteowl.strategy.TradingStrategyFactory;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class MockContext {
	
	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final TradingStrategyConfig config;
	
	private final MockBarService barService;
	private final QuoteService quoteService;
	private final ScripService scripService;
	private final PositionService positionService;
	private final TradingStrategy tradingStrategy;
	private final OptionChainService optionChainService;
	private final TradingStrategyFactory tradingStrategyFactory;
	
	@Builder
	public MockContext(
			@NonNull final Scrip scrip, 
			@NonNull final List<Bar> bars, 
			@NonNull final Timeframe timeframe,
			@NonNull final TradingStrategyConfig config) {
		this.bars = bars;
		this.scrip = scrip;
		this.config = config;
		this.timeframe = timeframe;
		
		this.quoteService = new MockQuoteService();
		this.scripService = new MockScripService(scrip);
		this.positionService = new MockPositionService();
		this.optionChainService = new MockOptionChainService();
		this.barService = new MockBarService(config.getMinBarCount() - 1, scrip.getCode(), bars, timeframe);
		this.tradingStrategyFactory = new TradingStrategyFactory(barService, scripService, optionChainService);
		this.tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
	}
	
	public BarService getBarService() {
		return barService;
	}
	
	public boolean next() {
		return barService.next();
	}

}
