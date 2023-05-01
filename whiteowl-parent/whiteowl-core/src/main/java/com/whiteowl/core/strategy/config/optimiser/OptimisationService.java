package com.whiteowl.core.strategy.config.optimiser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.backtester.BackTestReport;
import com.whiteowl.core.strategy.config.backtester.BackTestService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	private final BackTestService backTestService = new BackTestService();
	
	public TradingStrategyConfig optimise(String code, Timeframe timeframe, 
			Set<TradingStrategyConfig> configs, Function<String, Set<String>> splitter, 
			double initialMargin, double slippagePercentage) {
		final Map<TradingStrategyConfig, BackTestReport> reports = new ConcurrentHashMap<>();
		final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				code, timeframe, Integer.MAX_VALUE);
		configs.parallelStream().forEach(config -> {
			final List<Bar> bars = barSeries.getBarData();
			final BackTestReport report = backTestService.backtest(config, bars, initialMargin, slippagePercentage);
			reports.put(config, report);
		});
		return seekOptimumConfig(reports, splitter);
	}
	
	private TradingStrategyConfig seekOptimumConfig(
			Map<TradingStrategyConfig, BackTestReport> reports,
			Function<String, Set<String>> splitter) {
		
		return null;
	}

}
