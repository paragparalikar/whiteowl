package com.whiteowl.strategy.config;

import java.util.List;

import javax.validation.Valid;

import com.whiteowl.strategy.TradingStrategyConfig;

import lombok.NonNull;

public interface TradingStrategyConfigService {

	public TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config);

	public List<TradingStrategyConfig> findAll();
	
	public List<TradingStrategyConfig> findByEnabled(boolean value);
	
	public void deleteById(Long id);
	
}
