package com.whiteowl.strategy;

import com.whiteowl.core.scrip.ScripCriteria;

public interface TradingStrategyConfig {

	Long getId();
	
	void setId(Long id);
	
	boolean isEnabled();
	
	Integer getMinBarCount();
	
	ScripCriteria getScripCriteria();
	
	TradingStrategyTemplate getTemplate();
	
}
