package com.whiteowl.strategy.size;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface PositionSizingStrategy {

	double size(
			@NonNull final Position position, 
			@NonNull final Portfolio portfolio,
			@NonNull final TradingStrategyConfig config);
	
}
