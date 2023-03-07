package com.whiteowl.core.strategy.config;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

public interface TradingStrategyConfig {
	
	int getMinBarCount();
	
	String getScripCode();
	
	Timeframe getTimeframe();
	
	TradingStrategy createTradingStrategy(TradingStrategyContext context);
	
	public default String getId() {
		return String.join(java.io.File.separator, 
				getScripCode(), 
				getTimeframe().name(),
				getClass().getCanonicalName());
	}
}
