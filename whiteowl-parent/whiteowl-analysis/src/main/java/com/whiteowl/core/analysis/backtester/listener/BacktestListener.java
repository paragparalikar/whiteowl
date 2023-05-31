package com.whiteowl.core.analysis.backtester.listener;

import org.ta4j.core.Bar;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;

public interface BacktestListener {
	
	default void onQuote(Quote quote) {}
	
	default void onBar(Bar bar) {}
	
	default void onStart(Backtest backtest) {}
	
	default void onEntry(Position position) {}
	
	default void onExit(Position position) {}
	
	default void onEnd(Backtest backtest) {}

}
