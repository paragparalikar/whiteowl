package com.whiteowl.core.strategy.backtester;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class BackTestService {

	private final BarService barService;
	
	public double backtest(TradingStrategyConfig config) {
		return 0;
	}

}
