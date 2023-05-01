package com.whiteowl.core.strategy.config.optimiser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.backtester.BackTestReport;
import com.whiteowl.core.strategy.config.backtester.BackTestService;
import com.whiteowl.core.strategy.impl.MACDLongTradingStrategyConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	private final BackTestService backTestService = new BackTestService();
	
	public TradingStrategyConfig optimise(String code, Timeframe timeframe, 
			Set<TradingStrategyConfig> configs, double initialMargin, double slippagePercentage) {
		final Map<TradingStrategyConfig, Double> reports = new ConcurrentHashMap<>();
		final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				code, timeframe, Integer.MAX_VALUE);
		configs.parallelStream().forEach(config -> {
			final List<Bar> bars = barSeries.getBarData();
			final BackTestReport report = backTestService.backtest(config, bars, initialMargin, slippagePercentage);
			reports.put(config, report.getAnnualReturnsPct());
		});
		return seekOptimumConfig(reports);
	}
	
	private TradingStrategyConfig seekOptimumConfig(Map<TradingStrategyConfig, Double> reports) {
		final Map<String, Double> idReturns = reports.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue()));
		final Map<TradingStrategyConfig, Double> configAverageReturns = new HashMap<>();
		for(TradingStrategyConfig config : reports.keySet()) {
			final Double averageReturns = config.getNeighbours().stream()
				.map(TradingStrategyConfig::getId)
				.collect(Collectors.averagingDouble(idReturns::get));
			configAverageReturns.put(config, averageReturns);
		}
		return configAverageReturns.entrySet().stream()
				.max(Comparator.comparing(Entry::getValue))
				.map(Entry::getKey)
				.orElse(null);
	}
	
	
	private static  Set<TradingStrategyConfig> getOptimisationUniverse(String code, Timeframe timeframe) {
		final Set<TradingStrategyConfig> universe = new HashSet<>();
		for(int longBarCount = 13; longBarCount < 144; longBarCount++) {
			for(int shortBarCount = longBarCount / 4; shortBarCount <= longBarCount * 3 / 4; shortBarCount++) {
				for(int signalBarCount = 5; signalBarCount < 21; signalBarCount++) {
					universe.add(MACDLongTradingStrategyConfig.builder()
							.scripCode(code)
							.timeframe(timeframe)
							.longBarCount(longBarCount)
							.shortBarCount(shortBarCount)
							.signalBarCount(signalBarCount)
							.build());
				}
			}
		}
		return universe;
	}
	

}
