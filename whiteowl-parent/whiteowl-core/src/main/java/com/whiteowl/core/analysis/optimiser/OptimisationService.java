package com.whiteowl.core.analysis.optimiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	
	public TradingStrategyConfig optimise(List<Scrip> scrips, Timeframe timeframe, Set<TradingStrategyConfig> configs, 
			Function<TradingStrategyConfigPerformance, Double> optimisationFunction,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage) {
		log.info("Initiating optimisation for {} scrips and {} configs with offset {} and lookback {}",
				scrips.size(), configs.size(), offsetPeriod, lookbackPeriod);
		final Map<TradingStrategyConfig, List<Position>> configPositions = new ConcurrentHashMap<>();
		scrips.parallelStream().forEach(scrip -> {
			final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
					scrip.getCode(), timeframe, lookbackPeriod, offsetPeriod);
			final Backtest backtest = Backtest.builder()
					.scrip(scrip)
					.timeframe(timeframe)
					.bars(barSeries.getBarData())
					.initialMargin(initialMargin)
					.slippagePercentage(slippagePercentage)
					.build();
			configs.parallelStream()
				.collect(Collectors.toMap(Function.identity(), backtest::execute))
				.forEach((config, positions) -> configPositions
						.computeIfAbsent(config, key -> new ArrayList<>()).addAll(positions));
			log.info("Accumulated trades for scrip {} with all configs", scrip.getCode());
		});
		final Map<TradingStrategyConfig, TradingStrategyConfigPerformance> configPerformances = 
				configPositions.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, 
					entry -> new TradingStrategyConfigPerformance(initialMargin, entry.getValue())));
		return configPerformances.entrySet().stream()
				.max(Entry.comparingByValue(Comparator.comparing(optimisationFunction)))
				.map(Entry::getKey)
				.orElse(null);
	}
	
}
