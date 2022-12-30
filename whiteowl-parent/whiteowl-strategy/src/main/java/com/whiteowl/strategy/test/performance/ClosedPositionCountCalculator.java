package com.whiteowl.strategy.test.performance;

import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;

public class ClosedPositionCountCalculator {

	public double calculate(List<Bar> bars, Set<Position> positions) {
		return positions.stream()
				.filter(position -> PositionStatus.CLOSED.equals(position.getStatus()))
				.count();
	}

}
