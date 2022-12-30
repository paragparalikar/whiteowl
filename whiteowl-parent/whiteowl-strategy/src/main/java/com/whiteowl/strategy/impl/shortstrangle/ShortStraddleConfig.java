package com.whiteowl.strategy.impl.shortstrangle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortStraddleConfig implements TradingStrategyConfig {
	
	@PositiveOrZero private Integer quantity = 1;
	@PositiveOrZero private Double percentageStopLoss = 25D;
	@NotBlank private String entryCronExpression = "0 16 9 * * MON-FRI";
	@NotBlank private String exitCronExpression = "0 15 15 * * MON-FRI";
	
	private final int minBarCount = 0;
	private final Timeframe timeframe = Timeframe.D;
	private final String scripCode = Index.NIFTY50.getCode();
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;

}
