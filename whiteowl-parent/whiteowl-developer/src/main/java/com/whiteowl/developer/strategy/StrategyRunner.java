package com.whiteowl.developer.strategy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.whiteowl.developer.bar.BarRepository;
import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.indicator.Indicators;
import com.whiteowl.developer.trade.Trade;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StrategyRunner {
	private static final int LIMIT = 1000;
	
	private final BarRepository barRepository = BarRepository.getInstance();
	
	public <T> void run(Strategy<T> strategy, List<BarSeries> barSerieses) {
		final Map<T, List<Trade>> results = new ConcurrentHashMap<>();
		final Set<T> configs = strategy.getConfigs();
		for(int index = 0; index < barSerieses.size(); index++) {
			final BarSeries barSeries = barSerieses.get(index);
			final int barCount = LIMIT * barSeries.timeframe.getDayMultiple();
			barRepository.load(barSeries, barCount, Indicators.bars);
			Indicators.refresh();
			final AtomicInteger counter = new AtomicInteger();
			configs.parallelStream().forEach(config -> {
				final List<Trade> trades = strategy.execute(config);
				results.put(config, trades);
				counter.addAndGet(trades.size());
			});
			System.out.printf("%d / %d\t\tCollected %d trades for %s %s\n", 
					index, barSerieses.size(), counter.get(),
					barSeries.code, barSeries.timeframe.name());
		}
	}
	
}
