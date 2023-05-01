package com.whiteowl.core.strategy.config.performance;

import java.util.List;

public interface TradingStrategyConfigPerformanceService {

	void save(TradingStrategyConfigPerformance performance);
	
	TradingStrategyConfigPerformance findById(String id);
	
	List<TradingStrategyConfigPerformance> findAll();
	
}
