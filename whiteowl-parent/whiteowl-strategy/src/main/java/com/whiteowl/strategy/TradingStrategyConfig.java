package com.whiteowl.strategy;

import com.whiteowl.core.scrip.ScripCriteria;

public interface TradingStrategyConfig {

	String getId();
	
	boolean isEnabled();
	
	String getTradingStrategyId();
	
	ScripCriteria getScripCriteria();
	
}
