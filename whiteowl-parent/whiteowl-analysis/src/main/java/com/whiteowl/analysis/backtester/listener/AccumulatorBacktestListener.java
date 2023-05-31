package com.whiteowl.analysis.backtester.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.whiteowl.core.position.Position;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "uuid")
public class AccumulatorBacktestListener implements BacktestListener {

	private final UUID uuid = UUID.randomUUID();
	private final List<Position> positions = new ArrayList<>();

	@Override
	public void onExit(Position position) {
		positions.add(position);
	}
	
}
