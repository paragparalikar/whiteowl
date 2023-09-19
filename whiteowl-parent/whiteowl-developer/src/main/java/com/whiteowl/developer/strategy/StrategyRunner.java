package com.whiteowl.developer.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.whiteowl.developer.bar.Bar;
import com.whiteowl.developer.bar.BarRepository;
import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.indicator.Indicators;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StrategyRunner {
	private static final int LIMIT = 1000;
	
	private final List<Bar> bars = new ArrayList<>(LIMIT);
	private final Indicators indicators = new Indicators(bars);
	private final BarRepository barRepository = BarRepository.getInstance();
	private final ExecutorService executorService = Executors.newWorkStealingPool(100);
	
	public void run(Strategy strategy, List<BarSeries> barSerieses) {
		for(BarSeries barSeries : barSerieses) {
			final int barCount = LIMIT * barSeries.timeframe.getDayMultiple();
			barRepository.load(barSeries, barCount, bars);
			indicators.refresh();
			final CountDownLatch latch = new CountDownLatch(0);
		}
		
	}
	

}
