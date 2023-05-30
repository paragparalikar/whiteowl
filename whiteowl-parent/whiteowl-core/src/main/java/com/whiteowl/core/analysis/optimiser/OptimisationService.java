package com.whiteowl.core.analysis.optimiser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.analysis.backtester.listener.AccumulatorBacktestListener;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.util.Tuple2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	
	public TradingStrategyConfig optimise(List<Scrip> scrips, Timeframe timeframe, Set<TradingStrategyConfig> configs, 
			Function<TradingStrategyConfigPerformance, Double> optimisationFunction,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage) {
		final Map<TradingStrategyConfig, AccumulatorBacktestListener> configPositions = new ConcurrentHashMap<>();
		scrips.stream().forEach(scrip -> {
			final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
					scrip.getCode(), timeframe, lookbackPeriod, offsetPeriod);
			configs.parallelStream().forEach(config -> 
			Backtest.builder()
				.scrip(scrip)
				.timeframe(timeframe)
				.config(config)
				.bars(barSeries.getBarData())
				.initialMargin(initialMargin)
				.slippagePercentage(slippagePercentage)
				.build().execute(configPositions.computeIfAbsent(config, key -> new AccumulatorBacktestListener())));
			log.info("Accumulated trades for scrip {}", scrip.getCode());
		});
		return configPositions.entrySet().stream()
			.map(entry -> Tuple2.of(entry.getKey(), new TradingStrategyConfigPerformance(initialMargin, entry.getValue().getPositions())))
			.max(Comparator.comparing(Tuple2::getValue, Comparator.comparing(optimisationFunction)))
			.map(Tuple2::getKey)
			.orElse(null);
	}
	
}
