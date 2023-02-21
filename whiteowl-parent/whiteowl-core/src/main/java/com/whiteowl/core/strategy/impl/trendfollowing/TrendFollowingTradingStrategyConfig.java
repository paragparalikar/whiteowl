package com.whiteowl.core.strategy.impl.trendfollowing;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategyTemplate;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Data;
import lombok.NonNull;

@Data
public class TrendFollowingTradingStrategyConfig implements TradingStrategyConfig {

	@Positive private int smaBarCount = 20;
	@NotBlank @NonNull private String scripCode;
	@NonNull @NotNull private Timeframe timeframe = Timeframe.M15;
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.TREND_FOLLOWING;
	
	@Override
	public int getMinBarCount() {
		return smaBarCount + 1;
	}

}
