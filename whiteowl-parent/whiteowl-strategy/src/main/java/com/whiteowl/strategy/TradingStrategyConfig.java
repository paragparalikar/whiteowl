package com.whiteowl.strategy;

import com.whiteowl.core.scrip.ScripCriteria;

public interface TradingStrategyConfig {

	String getId();
	
	String getTradingStrategyId();
	
	ScripCriteria getScripCriteria();
	
	TradingStrategyType getTradingStrategyType();
	
	
}
