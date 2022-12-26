package com.whiteowl.strategy.meanReversion;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.Data;

@Data
public class MeanReversionInUptrendTradingStrategyConfig implements TradingStrategyConfig {

	@NotBlank private String id;
	private boolean enabled = true;
	@Positive private int rsiBarCount = 50;
	@PositiveOrZero @Max(100) private int minRsiValue = 50;
	@Positive private int macdShortBarCount = 12;
	@Positive private int macdLongBarCount = 26;
	@Positive private int stochasticBarCount = 14;
	@PositiveOrZero @Max(100) private int maxStochasticValue = 30;
	private TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.MEAN_REVERSION_UPTREND;

}
