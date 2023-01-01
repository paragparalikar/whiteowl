package com.whiteowl.strategy.size;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public class KellyPositionSizingStrategy implements PositionSizingStrategy {

	@Override
	public double size(
			@NonNull Position position,
			@NonNull Portfolio portfolio,
			@NonNull TradingStrategyConfig config) {
		return 0;
	}

}
