package com.whiteowl.core.analysis.optimiser;

import java.util.function.Function;

import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;

public interface OptimisationFunction extends Function<TradingStrategyConfigPerformance, Double> {

	
}
