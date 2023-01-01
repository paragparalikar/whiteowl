package com.whiteowl.strategy.test;

import java.util.List;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.performance.TradingStrategyPerformance;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class BackTestResult {

	@NonNull private final Scrip scrip;
	@NonNull private final Timeframe timeframe;
	@NonNull private final List<Position> positions;
	@NonNull private final TradingStrategyConfig config;
	@NonNull private final TradingStrategyPerformance performance;

}
