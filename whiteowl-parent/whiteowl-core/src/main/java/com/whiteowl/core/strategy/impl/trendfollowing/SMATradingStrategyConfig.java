package com.whiteowl.core.strategy.impl.trendfollowing;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Data;

@Data
public class SMATradingStrategyConfig implements TradingStrategyConfig {

	private String scripCode;
	private Timeframe timeframe;

	private int barCount = 21;

	@Override
	public int getMinBarCount() {
		return barCount + 1;
	}
	
	@Override
	public TradingStrategy createTradingStrategy(TradingStrategyContext context) {
		return new SMATradingStrategy(this, context);
	}
	
}
