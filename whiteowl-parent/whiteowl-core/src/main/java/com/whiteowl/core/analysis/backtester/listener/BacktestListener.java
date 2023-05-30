package com.whiteowl.core.analysis.backtester.listener;

import java.util.function.Consumer;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.position.Position;

public interface BacktestListener extends Consumer<Position> {
	
	default void onStart(Backtest backtest) {}
	
	default void accept(Position position) {}
	
	default void onEnd(Backtest backtest) {}

}
