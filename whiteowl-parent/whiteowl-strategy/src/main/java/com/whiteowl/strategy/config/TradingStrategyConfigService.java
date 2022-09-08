package com.whiteowl.strategy.config;

import java.util.List;

import com.whiteowl.strategy.TradingStrategyConfig;

public interface TradingStrategyConfigService {

	public TradingStrategyConfig save(TradingStrategyConfig config);

	public List<TradingStrategyConfig> findAll();
	
	public List<TradingStrategyConfig> findByEnabled(boolean value);
	
	public void deleteById(Long id);
	
}
