package com.whiteowl.core.analysis.walkForward;

import com.whiteowl.core.strategy.config.TradingStrategyConfig;

public interface WalkForwardListener {
	
	default void onStart(WalkForwardConfig config) {}
	
	default void onTrainStart(int stepIndex) {}
	
	default void onTrainEnd(int stepIndex, TradingStrategyConfig selectedConfig) {}
	
	default void onTestStart(int stepIndex, TradingStrategyConfig selectedConfig) {}
	
	default void onTestEnd(WalkForwardStep step) {}
	
	default void onEnd() {}

}
