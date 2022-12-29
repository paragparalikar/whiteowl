package com.whiteowl.strategy.donchian;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.Data;

@Data
public class DonchianBreakoutTradingStrategyConfig implements TradingStrategyConfig {

	@NotBlank private String id;
	@Positive private int lowerBandLength = 20;
	@Positive private int upperBandLength = 20;
	@Positive private double priceTargetPercentage = 10;
	@Positive private double initialPriceStopPercentage = 5;
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.DONCHIAN;

}
