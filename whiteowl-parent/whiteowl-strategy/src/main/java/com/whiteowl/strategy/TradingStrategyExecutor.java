package com.whiteowl.strategy;

import com.whiteowl.strategy.config.TradingStrategyConfig;

public interface TradingStrategyExecutor {
	
	void execute(TradingStrategyConfig config);

}
