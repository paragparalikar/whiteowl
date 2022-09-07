package com.whiteowl.strategy;

import com.whiteowl.core.scrip.ScripCriteria;

public interface TradingStrategyConfig {

	String getId();
	
	boolean isEnabled();
	
	Integer getMinBarCount();
	
	String getTradingStrategyId();
	
	ScripCriteria getScripCriteria();
	
}
