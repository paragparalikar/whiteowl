package com.whiteowl.strategy.test.performance;

import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;

public class WinRateCalculator {
	
	private final TotalPositionCountCalculator totalPositionCountCalculator = 
			new TotalPositionCountCalculator();
	private final WinningPositionCountCalculator winningPositionCountCalculator = 
			new WinningPositionCountCalculator();

	public double calculate(List<Bar> bars, Set<Position> positions) {
		final double totalPositionCount = totalPositionCountCalculator.calculate(bars, positions);
		final double winningPositionCount = winningPositionCountCalculator.calculate(bars, positions);
		return 0 == totalPositionCount ? 0 : winningPositionCount / totalPositionCount;
	}

}
