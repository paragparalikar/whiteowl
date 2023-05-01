package com.whiteowl.core.strategy.impl;

import java.util.HashSet;
import java.util.Set;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
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
	
	@Override
	public Set<TradingStrategyConfig> getNeighbours() {
		final Set<TradingStrategyConfig> neighbours = new HashSet<>();
		for(int longBarCount = getLongBarCount() - 7; longBarCount <= getLongBarCount() + 7; longBarCount++) {
			for(int shortBarCount = getShortBarCount() - 5; shortBarCount <= getShortBarCount() + 5; shortBarCount++) {
				for(int signalBarCount = getSignalBarCount() - 3; signalBarCount <= getSignalBarCount() + 3; signalBarCount++) {
					neighbours.add(MACDLongTradingStrategyConfig.builder()
							.scripCode(getScripCode())
							.timeframe(getTimeframe())
							.longBarCount(longBarCount)
							.shortBarCount(shortBarCount)
							.signalBarCount(signalBarCount)
							.build());
				}
			}
		}
		return neighbours;
	}
	
}
