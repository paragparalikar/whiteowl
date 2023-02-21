package com.whiteowl.core.strategy.size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

@Component
public class FixedPercentagePositionSizingStrategy implements PositionSizingStrategy {

	private final double fixedPositionSizePercentage;
	
	public FixedPercentagePositionSizingStrategy(
			@Value("${whiteowl.strategy.size.fixed.percentage:5}") 
			final double fixedPositionSizePercentage) {
		this.fixedPositionSizePercentage = fixedPositionSizePercentage;
	}
	
	@Override
	public double size(
			@NonNull Position position, 
			@NonNull Portfolio portfolio,
			@NonNull TradingStrategyConfig config) {
		final double maxTradableAmount = portfolio.getMaxTradableAmount();
		final double maxPositionSize = maxTradableAmount * fixedPositionSizePercentage / 100;
		final double avaialbleMargin = portfolio.getAvailableMargin();
		return Math.min(avaialbleMargin, maxPositionSize);
	}

}
