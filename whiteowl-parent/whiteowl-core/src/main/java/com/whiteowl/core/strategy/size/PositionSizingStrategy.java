package com.whiteowl.core.strategy.size;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface PositionSizingStrategy {

	double size(
			@NonNull final Position position, 
			@NonNull final Portfolio portfolio,
			@NonNull final TradingStrategyConfig config);
	
}
