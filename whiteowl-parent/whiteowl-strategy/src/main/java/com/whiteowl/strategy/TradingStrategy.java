package com.whiteowl.strategy;

import com.whiteowl.core.position.Position;

public interface TradingStrategy {

	boolean handle(Position position);
	
}
