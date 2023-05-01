package com.whiteowl.core.strategy.backtester;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class BackTestService {

	public double backtest(TradingStrategyConfig config, List<Bar> bars, double initialMargine, double slippagePercentage) {
		final TradingStrategyContext tradingStrategyContext = new MockTradingStrategyContext(initialMargine, slippagePercentage);
		
		
		return 0;
	}

}
