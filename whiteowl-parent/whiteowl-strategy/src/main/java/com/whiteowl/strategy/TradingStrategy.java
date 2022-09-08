package com.whiteowl.strategy;

import com.whiteowl.core.position.Position;

public interface TradingStrategy {

	void handle(Position position);
	
}
