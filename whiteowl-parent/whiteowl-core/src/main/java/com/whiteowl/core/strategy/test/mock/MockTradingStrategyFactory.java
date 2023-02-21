package com.whiteowl.core.strategy.test.mock;

import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.TradingStrategyFactory;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public class MockTradingStrategyFactory extends TradingStrategyFactory {

	private final TradingStrategyConfig config;
	private final TradingStrategy tradingStrategy;
	
	public MockTradingStrategyFactory(
			@NonNull final TradingStrategyConfig config,
			@NonNull final TradingStrategy tradingStrategy) {
		super(null, null, null);
		this.config = config;
		this.tradingStrategy = tradingStrategy;
	}

	public TradingStrategy getTradingStrategy(@NonNull final TradingStrategyConfig config) {
		if(!config.equals(this.config)) throw new IllegalArgumentException();
		return tradingStrategy;
	};
	
}
