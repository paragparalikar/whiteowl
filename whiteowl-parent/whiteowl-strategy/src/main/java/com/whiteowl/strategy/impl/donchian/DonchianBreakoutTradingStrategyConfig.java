package com.whiteowl.strategy.impl.donchian;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.Data;
import lombok.NonNull;

@Data
public class DonchianBreakoutTradingStrategyConfig implements TradingStrategyConfig {

	@NotBlank @NonNull private String scripCode;
	@NotNull @NonNull private Timeframe timeframe;
	@Positive private int lowerBandLength = 20;
	@Positive private int upperBandLength = 20;
	@Positive private double priceTargetPercentage = 10;
	@Positive private double initialPriceStopPercentage = 5;
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.DONCHIAN;

	@Override
	public int getMinBarCount() {
		return Math.max(upperBandLength, lowerBandLength) + 2;
	}
	
}
