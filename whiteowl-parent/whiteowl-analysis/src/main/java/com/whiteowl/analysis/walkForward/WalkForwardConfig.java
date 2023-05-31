package com.whiteowl.analysis.walkForward;

import java.util.List;
import java.util.Set;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WalkForwardConfig {

	private final List<Scrip> scrips;
	private final Timeframe timeframe;
	private final Set<TradingStrategyConfig> configs;
	private final int trainBarCount, testBarCount, steps;
	private final double initialMargin, slippagePercentage;

}
