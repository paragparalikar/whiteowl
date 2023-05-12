package com.whiteowl.core.analysis.optimiser;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.ta4j.core.Bar;

import com.whiteowl.core.analysis.backtester.BackTestService;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OptimisationService {

	private final BarService barService;
	private final BackTestService backTestService = new BackTestService();
	
	public TradingStrategyConfig optimise(List<String> codes, Timeframe timeframe, 
			Set<TradingStrategyConfig> configs, OptimisationFunction optimisationFunction,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage) {
		final Map<TradingStrategyConfig, TradingStrategyConfigPerformance> performances = new ConcurrentHashMap<>();
		configs.stream().forEach(config -> {
			final TradingStrategyConfigPerformance performance = evaluate(codes, timeframe, config, 
					offsetPeriod, lookbackPeriod, initialMargin, slippagePercentage);
			performances.put(config, performance);
		});
		return performances.entrySet().stream()
				.max(Entry.comparingByValue(Comparator.comparing(optimisationFunction)))
				.map(Entry::getKey)
				.orElse(null);
	}
	
	private TradingStrategyConfigPerformance evaluate(List<String> codes, Timeframe timeframe,
			TradingStrategyConfig config, int offsetPeriod, int lookbackPeriod, double initialMargin, 
			double slippagePercentage) {
		final List<Position> positions = codes.parallelStream()
				.map(code -> evaluate(code, timeframe, config, offsetPeriod, lookbackPeriod, initialMargin, slippagePercentage))
				.flatMap(Collection::stream)
				.sorted(Comparator.comparing(Position::getCreatedDate))
				.collect(Collectors.toList());
		return new TradingStrategyConfigPerformance(initialMargin, positions);
	}
	
	private List<Position> evaluate(String code, Timeframe timeframe, TradingStrategyConfig config,
			int offsetPeriod, int lookbackPeriod, double initialMargin, double slippagePercentage){
		final List<Bar> bars = null; // barService.get
		return backTestService.backtest(config, bars, initialMargin, slippagePercentage);
	}
	
}
