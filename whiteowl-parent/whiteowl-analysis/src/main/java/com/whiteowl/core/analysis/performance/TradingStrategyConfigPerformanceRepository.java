package com.whiteowl.core.analysis.performance;

import java.util.List;

public interface TradingStrategyConfigPerformanceRepository {

	void save(TradingStrategyConfigPerformance performance);
	
	TradingStrategyConfigPerformance findById(String id);
	
	List<TradingStrategyConfigPerformance> findAll();
	
}
