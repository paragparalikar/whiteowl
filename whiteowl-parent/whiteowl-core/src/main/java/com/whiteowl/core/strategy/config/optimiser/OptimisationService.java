package com.whiteowl.core.strategy.config.optimiser;

import java.util.Set;

import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.backtester.BackTestService;

public class OptimisationService {

	private final BackTestService backTestService = new BackTestService();
	
	public void optimise(Set<TradingStrategyConfig> configs) {
		
	}

}
