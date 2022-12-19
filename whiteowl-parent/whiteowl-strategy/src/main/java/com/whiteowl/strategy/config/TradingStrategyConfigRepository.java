package com.whiteowl.strategy.config;

import java.util.List;
import java.util.Optional;

public interface TradingStrategyConfigRepository {
	
	long count();
	
	Optional<TradingStrategyConfig> findById(String id);
	
	TradingStrategyConfig save(TradingStrategyConfig config);

	List<TradingStrategyConfig> findByEnabled(boolean value);
	
}
