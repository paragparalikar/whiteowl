package com.whiteowl.strategy.config;

import java.util.Collections;
import java.util.Set;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.strategy.TradingStrategyTemplate;

public interface TradingStrategyConfig {
	
	int getMinBarCount();
	
	String getScripCode();
	
	Timeframe getTimeframe();
	
	TradingStrategyTemplate getTradingStrategyTemplate();
	
	default Set<String> getCronExpressions() { return Collections.emptySet(); }
	
	default String getId() {
		return String.join("-", getScripCode(), getTimeframe().name(), getTradingStrategyTemplate().name());
	}
	
}
