package com.whiteowl.core.analysis.backtester;

import java.util.ArrayList;
import java.util.List;

import com.whiteowl.core.position.Position;

import lombok.Getter;

@Getter
public class AccumulatorBacktestListener implements BacktestListener {

	private final List<Position> positions = new ArrayList<>();

	@Override
	public void accept(Position position) {
		positions.add(position);
	}
	
}
