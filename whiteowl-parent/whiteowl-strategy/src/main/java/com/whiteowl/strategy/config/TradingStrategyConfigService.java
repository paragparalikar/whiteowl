package com.whiteowl.strategy.config;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.NonNull;

public interface TradingStrategyConfigService {
	
	long count();
	
	Optional<TradingStrategyConfig> findById(@NonNull String id);

	TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config);

	List<TradingStrategyConfig> findByEnabled(boolean value);
	
	List<TradingStrategyConfig> findByEnabledAndTemplate(boolean enabled, TradingStrategyTemplate template);
	
}
