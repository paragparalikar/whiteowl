package com.whiteowl.core.strategy.impl;

import java.util.Collections;
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
	public Set<TradingStrategyConfig> getOptimisationUniverse() {
		return Collections.emptySet();
	}
	
	@Override
	public Set<TradingStrategyConfig> getNeighbours() {
		final int step = 2;
		final double percentage = 4;
		final Set<TradingStrategyConfig> neighbours = new HashSet<>();
		final int longBarCountSpan = span(getLongBarCount(), step, percentage);
		final int shortBarCountSpan = span(getShortBarCount(), step, percentage);
		final int signalBarCountSpan = span(getSignalBarCount(), step, percentage);
		for(int longBarCount = getLongBarCount() - longBarCountSpan; longBarCount <= getLongBarCount() + longBarCountSpan; longBarCount += step) {
			for(int shortBarCount = getShortBarCount() - shortBarCountSpan; shortBarCount <= getShortBarCount() + shortBarCountSpan; shortBarCount += step) {
				for(int signalBarCount = getSignalBarCount() - signalBarCountSpan; signalBarCount <= getSignalBarCount() + signalBarCountSpan; signalBarCount += step) {
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
	
	private int span(int value, int step, double percentage) {
		final double limit = value * percentage / 100d;
		return ((int)(limit / step)) * step;
	}
	
}
