package com.whiteowl.analysis.performance;

import java.util.List;

public interface TradingStrategyConfigPerformanceService {

	void save(TradingStrategyConfigPerformance performance);
	
	TradingStrategyConfigPerformance findById(String id);
	
	List<TradingStrategyConfigPerformance> findAll();
	
}
