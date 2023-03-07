package com.whiteowl.core.strategy.config;

import java.util.List;

import javax.validation.Valid;

import lombok.NonNull;

public interface TradingStrategyConfigRepository {
	
	List<TradingStrategyConfig> findAll();
	
	TradingStrategyConfig save(
			@NonNull @Valid final TradingStrategyConfig config);
	
}
