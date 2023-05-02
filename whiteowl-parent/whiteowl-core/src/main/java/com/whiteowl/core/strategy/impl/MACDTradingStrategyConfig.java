package com.whiteowl.core.strategy.impl;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public abstract class MACDTradingStrategyConfig implements TradingStrategyConfig {
	
	private String scripCode;
	private Timeframe timeframe;
	private TradeType tradeType;
	private int longBarCount, shortBarCount, signalBarCount;

	@Override
	public int getMinBarCount() {
		return longBarCount + 1;
	}
	
	public String getId() {
		return String.join("-", 
				"MACD",
				String.valueOf(tradeType),
				String.valueOf(scripCode), 
				String.valueOf(timeframe),
				String.valueOf(longBarCount),
				String.valueOf(shortBarCount),
				String.valueOf(signalBarCount));
	}
	
}
