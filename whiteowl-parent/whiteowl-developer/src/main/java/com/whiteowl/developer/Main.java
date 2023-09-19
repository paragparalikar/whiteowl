package com.whiteowl.developer;

import java.util.ArrayList;
import java.util.List;

import com.whiteowl.developer.bar.BarRepository;
import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.bar.Timeframe;
import com.whiteowl.developer.strategy.StochasticsMeanReversionStrategy;
import com.whiteowl.developer.strategy.StrategyRunner;

public class Main {
	
	public static void main(String[] args) {
		final BarRepository barRepository = BarRepository.getInstance();
		final StochasticsMeanReversionStrategy strategy = new StochasticsMeanReversionStrategy();
		final List<String> codes = barRepository.findAllCodes();
		final List<BarSeries> serieses = new ArrayList<>(Timeframe.values().length * codes.size());
		for(Timeframe timeframe : Timeframe.values()) {
			for(String code : codes) {
				serieses.add(new BarSeries(code, timeframe));
			}
		}
		
		final StrategyRunner strategyRunner = new StrategyRunner();
		strategyRunner.run(strategy, serieses);
	}

}
