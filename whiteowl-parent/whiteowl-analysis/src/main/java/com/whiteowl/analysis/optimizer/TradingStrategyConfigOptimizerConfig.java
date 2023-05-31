package com.whiteowl.analysis.optimizer;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.whiteowl.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TradingStrategyConfigOptimizerConfig {
	
	private final List<Scrip> scrips;
	private final Timeframe timeframe;
	private final Set<TradingStrategyConfig> configs;
	private final int offsetPeriod, lookbackPeriod; 
	private final double initialMargin, slippagePercentage;
	private final Function<TradingStrategyConfigPerformance, Double> optimisationFunction;

}
