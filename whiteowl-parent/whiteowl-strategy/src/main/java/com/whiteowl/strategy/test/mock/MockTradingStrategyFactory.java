package com.whiteowl.strategy.test.mock;

import com.whiteowl.strategy.TradingStrategy;
import com.whiteowl.strategy.TradingStrategyFactory;
import com.whiteowl.strategy.config.TradingStrategyConfig;

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
