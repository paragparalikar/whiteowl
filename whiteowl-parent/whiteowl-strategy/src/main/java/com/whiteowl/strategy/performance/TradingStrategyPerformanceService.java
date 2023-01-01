package com.whiteowl.strategy.performance;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface TradingStrategyPerformanceService {

	TradingStrategyPerformance calculate(
			@NonNull List<Bar> bars, 
			@NonNull List<Position> positions,
			@NonNull List<Double> equityCurve, 
			@NonNull TradingStrategyConfig config);

}
