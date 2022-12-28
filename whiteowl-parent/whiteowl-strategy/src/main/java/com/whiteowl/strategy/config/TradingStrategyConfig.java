package com.whiteowl.strategy.config;

import com.whiteowl.strategy.TradingStrategyTemplate;

public interface TradingStrategyConfig {
	
	String getId();
	
	int getBarCount();

	boolean isEnabled();
	
	TradingStrategyTemplate getTradingStrategyTemplate();
	
}
