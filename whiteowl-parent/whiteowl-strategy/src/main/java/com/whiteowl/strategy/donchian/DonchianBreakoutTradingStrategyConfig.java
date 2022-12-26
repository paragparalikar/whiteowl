package com.whiteowl.strategy.donchian;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.model.InitialPriceStopType;
import com.whiteowl.strategy.model.PriceTargetType;

import lombok.Data;
import lombok.NonNull;

@Data
public class DonchianBreakoutTradingStrategyConfig implements TradingStrategyConfig {

	@NotBlank private String id;
	private boolean enabled = true;
	@Positive private int lowerBandLength = 20;
	@Positive private int upperBandLength = 20;
	@Positive private double priceTargetValue = 2;
	@Positive private double initialPriceStopValue = 5;
	@NotNull @NonNull private PriceTargetType priceTargetType = PriceTargetType.RISK_MULTIPLE;
	@NotNull @NonNull private InitialPriceStopType initialPriceStopType = InitialPriceStopType.PERCENTAGE;
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.DONCHIAN;

}
