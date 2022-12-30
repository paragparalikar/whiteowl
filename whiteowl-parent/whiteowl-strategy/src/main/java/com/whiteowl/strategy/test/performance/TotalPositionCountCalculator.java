package com.whiteowl.strategy.test.performance;

import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;

public class TotalPositionCountCalculator {

	public double calculate(List<Bar> bars, Set<Position> positions) {
		return positions.size();
	}

}
