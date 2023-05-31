package com.whiteowl.core.analysis.optimizer;

import com.whiteowl.core.strategy.config.TradingStrategyConfig;

public interface TradingStrategyConfigOptimizationListener {
	
	TradingStrategyConfigOptimizationListener NULL = new TradingStrategyConfigOptimizationListener() {};

	default void onStart(TradingStrategyConfigOptimizerConfig config) {}
	
	default void onEnd(TradingStrategyConfig selectedConfig) {}
}
