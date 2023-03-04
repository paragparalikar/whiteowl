package com.whiteowl.core.strategy.impl.trendfollowing;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategyTemplate;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Data;

@Data
public class SMATradingStrategyConfig implements TradingStrategyConfig {

	private String scripCode;
	private Timeframe timeframe;
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SMA;

	private int barCount = 21;

	@Override
	public int getMinBarCount() {
		return barCount + 1;
	}
}
