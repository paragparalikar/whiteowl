package com.whiteowl.strategy.test;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.test.mock.MockBarService;
import com.whiteowl.strategy.test.mock.MockPositionService;
import com.whiteowl.strategy.test.mock.MockQuoteService;
import com.whiteowl.strategy.test.mock.MockTaskScheduler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	private final BarService barService;
	
	public <T extends TradingStrategyConfig> void test(
			@NonNull final T config, 
			@NonNull final Scrip scrip, 
			@NonNull final Timeframe timeframe) {
		final List<Bar> bars = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		final MockBarService barService = new MockBarService();
		barService.setIndex(0);
		barService.setBars(bars);
		barService.setTimeframe(timeframe);
		barService.setCode(scrip.getCode());
		final MockQuoteService quoteService = new MockQuoteService();
		final MockPositionService positionService = new MockPositionService();
		final MockTaskScheduler taskScheduler = new MockTaskScheduler();
		
		
	}
	
	public <T extends TradingStrategyConfig> void test(
			@NonNull final T config, 
			@NonNull final Scrip scrip,
			@NonNull final TradingStrategyExecutor<T> executor) {
		
		
	}
	

	
}
