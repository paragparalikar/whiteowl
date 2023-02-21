package com.whiteowl.core.strategy.config;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategyTemplate;

public interface TradingStrategyConfig {
	
	int getMinBarCount();
	
	String getScripCode();
	
	Timeframe getTimeframe();
	
	TradingStrategyTemplate getTradingStrategyTemplate();
	
	default String getExitCronExpression() { return null; }
	
	default String getEntryCronExpression() { return null; }
	
	default String getId() {
		return String.join("-", getScripCode(), getTimeframe().name(), getTradingStrategyTemplate().name());
	}
	
}
