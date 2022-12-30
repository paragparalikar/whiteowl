package com.whiteowl.strategy.test;

import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class BackTestResult {

	@NonNull private final Scrip scrip;
	@NonNull private final List<Bar> bars;
	@NonNull private final Timeframe timeframe;
	@NonNull private final Set<Position> positions;
	
	private final int totalPositionCount;
	private final int openPositionCount;
	private final int closedPositionCount;
	private final double cagr;
	private final double alpha;
	private final double beta;
	private final double winRate; 
	private final double expectancy;
	private final double sharpeRatio; 
	private final double profitFactor; 
	private final double maxDrawdown;

}
