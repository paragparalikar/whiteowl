package com.whiteowl.core.strategy.config;

import java.util.List;

import lombok.NonNull;

public interface TradingStrategyConfigService {
	
	TradingStrategyConfig save(
			@NonNull final TradingStrategyConfig config);
	
	List<TradingStrategyConfig> findAll();
	
}
