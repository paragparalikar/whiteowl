package com.whiteowl.strategy.test;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.TradingStrategy;
import com.whiteowl.strategy.TradingStrategyFactory;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.test.mock.MockBarService;
import com.whiteowl.strategy.test.mock.MockOptionChainService;
import com.whiteowl.strategy.test.mock.MockScripService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	private final BarService barService;
	
	public void test(
			@NonNull final Scrip scrip, 
			@NonNull final Timeframe timeframe,
			@NonNull final TradingStrategyConfig config) {
		final MockScripService scripService = new MockScripService(scrip);
		final MockOptionChainService optionChainService = new MockOptionChainService();
		final List<Bar> bars = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		if(bars.size() < config.getMinBarCount()) return;
		final MockBarService barService = new MockBarService(
				config.getMinBarCount() - 1, scrip.getCode(), bars, timeframe);
		final TradingStrategyFactory tradingStrategyFactory = new TradingStrategyFactory(
				barService, scripService, optionChainService);
		final TradingStrategy tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
		
		while(barService.next()) {
			tradingStrategy.enter().ifPresent(null);
		}
		
	}
	
	

	
}
