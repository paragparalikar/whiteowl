package com.whiteowl.core.strategy.optimiser;

import java.util.Set;

import com.whiteowl.core.strategy.backtester.BackTestService;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

public class OptimisationService {

	private final BackTestService backTestService = new BackTestService();
	
	public void optimise(Set<TradingStrategyConfig> configs) {
		
	}

}
