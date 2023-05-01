package com.whiteowl.core.strategy.impl;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Builder;

public class MACDLongTradingStrategyConfig extends MACDTradingStrategyConfig {

	@Builder
	public MACDLongTradingStrategyConfig(String scripCode, Timeframe timeframe, int longBarCount,
			int shortBarCount, int signalBarCount) {
		super(scripCode, timeframe, TradeType.BUY, longBarCount, shortBarCount, signalBarCount);
	}

	@Override
	public TradingStrategy createTradingStrategy(TradingStrategyContext context) {
		return new MACDLongTradingStrategy(this, context);
	}
	
}
