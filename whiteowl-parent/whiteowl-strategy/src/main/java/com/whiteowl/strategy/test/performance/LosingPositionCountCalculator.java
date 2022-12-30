package com.whiteowl.strategy.test.performance;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.position.Position;

public class LosingPositionCountCalculator {

	public double calculate(List<Bar> bars, Set<Position> positions) {
		return positions.stream()
				.filter(this::isLosing)
				.count();
	}
	
	private boolean isLosing(Position position) {
		final TradeType entryTradeType = Optional.ofNullable(position.getEntryTradeType())
				.orElse(TradeType.BUY);
		return 0 > (TradeType.BUY.equals(entryTradeType) ?
				position.getEntryAmount() - position.getExitAmount():
				position.getExitAmount() - position.getEntryAmount());
	}

}
