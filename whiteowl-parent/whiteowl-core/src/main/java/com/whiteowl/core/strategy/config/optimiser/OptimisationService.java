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
import com.whiteowl.core.bar.DefaultBarService;
import com.whiteowl.core.bar.JdbcBarRepository;
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
			System.out.println(report.getAnnualReturnsPct());
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
		final TradingStrategyConfig config = configAverageReturns.entrySet().stream()
				.max(Comparator.comparing(Entry::getValue))
				.map(Entry::getKey)
				.orElse(null);
		System.err.printf("Optimum config %s - Avg Annual Returns %f\n", config.toString(), configAverageReturns.get(config));
		return config;
	}
	
	public static void main(String[] args) {
		final String code = "RELIANCE";
		final Timeframe timeframe = Timeframe.M15;
		final BarService barService = new DefaultBarService(JdbcBarRepository.instance());
		final OptimisationService optimisationService = new OptimisationService(barService);
		final Set<TradingStrategyConfig> configs = getOptimisationUniverse(code, timeframe);
		System.out.println("Optimisation universe is of size : " + configs.size());
		final TradingStrategyConfig config = optimisationService.optimise(code, timeframe, configs, 100_00_000, 0.5);
		System.err.println("Optimum config : " + config.toString());
	}
	
	private static  Set<TradingStrategyConfig> getOptimisationUniverse(String code, Timeframe timeframe) {
		final Set<TradingStrategyConfig> universe = new HashSet<>();
		for(int longBarCount = 20; longBarCount <= 200; longBarCount += 2) {
			for(int shortBarCount = trim(longBarCount / 4, 2); shortBarCount <= trim(longBarCount * 3 / 4, 2); shortBarCount += 2) {
				for(int signalBarCount = 4; signalBarCount <= 22; signalBarCount += 2) {
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
	
	private static int trim(int value, int step) {
		return ((int)(value / step)) * step;
	}

}
