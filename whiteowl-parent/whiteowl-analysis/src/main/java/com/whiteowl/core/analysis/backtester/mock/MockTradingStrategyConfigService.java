package com.whiteowl.core.analysis.backtester.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.TradingStrategyConfigService;

import lombok.NonNull;

public class MockTradingStrategyConfigService implements TradingStrategyConfigService {

	private final Set<TradingStrategyConfig> cache = new HashSet<>();

	@Override
	public TradingStrategyConfig save(@NonNull TradingStrategyConfig config) {
		cache.add(config);
		return config;
	}

	@Override
	public List<TradingStrategyConfig> findAll() {
		return new ArrayList<>(cache);
	}

}
