package com.whiteowl.core.strategy.config.optimiser;

import java.util.Set;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.backtester.BackTestService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	private final BackTestService backTestService = new BackTestService();
	
	public void optimise(String code, Timeframe timeframe, Set<TradingStrategyConfig> configs) {
		final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				code, timeframe, Integer.MAX_VALUE);
	}

}
