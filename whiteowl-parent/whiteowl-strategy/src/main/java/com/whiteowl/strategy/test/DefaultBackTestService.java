package com.whiteowl.strategy.test;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.performance.TradingStrategyPerformanceService;
import com.whiteowl.strategy.test.mock.MockContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	private final BarService barService;
	private final ScripService scripService;
	private final TradingStrategyPerformanceService tradingStrategyPerformaceService;
	
	public BackTestResult test(@NonNull final TradingStrategyConfig config) {
		final Timeframe timeframe = config.getTimeframe();
		final Scrip scrip = scripService.findByCode(config.getScripCode());
		final BarSeries barSeries = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		final MockContext context = new MockContext(scrip, barSeries, timeframe, config);
		while(context.next()) {
			
			
		}
		
		
		return null;
	}
	
	

	
}
