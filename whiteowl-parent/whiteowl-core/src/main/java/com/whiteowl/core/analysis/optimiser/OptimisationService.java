package com.whiteowl.core.analysis.optimiser;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.ta4j.core.Bar;

import com.whiteowl.core.analysis.backtester.BackTestReport;
import com.whiteowl.core.analysis.backtester.BackTestService;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.DefaultBarService;
import com.whiteowl.core.bar.JdbcBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.impl.MACDLongTradingStrategyConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	private final BackTestService backTestService = new BackTestService();
	
	public TradingStrategyConfig optimise(List<String> codes, Timeframe timeframe, 
			Set<TradingStrategyConfig> configs, OptimisationFunction optimisationFunction,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage) {
		final Map<TradingStrategyConfig, Double> ranks = new ConcurrentHashMap<>();
		configs.stream().forEach(config -> {
			codes.parallelStream()
				.map(code -> backtest(code, timeframe, config, offsetPeriod, lookbackPeriod, initialMargin, slippagePercentage))
				.flatMap(Collection::stream)
				.sorted(Comparator.comparing(null))
				.collect(Collectors.toList());
		});
		return null;
	}
	
	private List<Position> backtest(String code, Timeframe timeframe, TradingStrategyConfig config,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage){
		final List<Bar> bars = null; // barService.get
		return backTestService.backtest(config, bars, initialMargin, slippagePercentage);
	}
	
	public TradingStrategyConfig optimise(List<Bar> bars, Set<TradingStrategyConfig> configs, 
			double initialMargin, double slippagePercentage) {
		final AtomicInteger counter = new AtomicInteger();
		final Map<TradingStrategyConfig, Double> reports = new ConcurrentHashMap<>();
		
		configs.parallelStream().forEach(config -> {
			final Duration duration = Duration.between(bars.get(0).getBeginTime(), bars.get(bars.size() - 1).getEndTime());
			final List<Position> positions = backTestService.backtest(config, bars, 100_00_000, slippagePercentage);
			
			final double annualReturnsPct = BackTestReport.computeAnnualReturnsPct(config, duration, positions);
			reports.put(config, annualReturnsPct);
			final int count = counter.incrementAndGet();
			System.out.printf("Index : %d out of %d, Positions : %d, Annual Percentage Returns : %f, id : %s\n",
					count, configs.size(), 
					positions.size(), 
					annualReturnsPct,
					config.getId());
			if(0 == (count % 1000)) printMinMax(reports);
		});
		return seekOptimumConfig(reports);
	}
	
	private void printMinMax(Map<TradingStrategyConfig, Double> reports) {
		double minReturns = 0, maxReturns = 0;
		TradingStrategyConfig min = null, max = null;
		for(TradingStrategyConfig config : reports.keySet()) {
			final double returns = reports.getOrDefault(config, 0d);
			if(null == min || minReturns > returns) {
				minReturns = returns;
				min = config;
			}
			if(null == max || maxReturns < returns) {
				maxReturns = returns;
				max = config;
			}
		}
		System.out.printf("Min : %s Min returns : %f | Max : %s Max returns : %f\n", min.getId(), minReturns, max.getId(), maxReturns);
	}
	
	private TradingStrategyConfig seekOptimumConfig(Map<TradingStrategyConfig, Double> reports) {
		printMinMax(reports);
		final Map<String, Double> idReturns = reports.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue()));
		final Map<TradingStrategyConfig, Double> configAverageReturns = new HashMap<>();
		for(TradingStrategyConfig config : reports.keySet()) {
			final Double averageReturns = config.getNeighbours().stream()
				.map(TradingStrategyConfig::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.averagingDouble(id -> idReturns.getOrDefault(id, 0d)));
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
		final TradingStrategyConfig config = optimisationService.optimise(code, timeframe, configs, 
				500 * timeframe.getDayMultiple(), 100_00_000, 0);
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
