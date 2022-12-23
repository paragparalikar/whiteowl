package com.whiteowl.strategy.shortstrangle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

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
	
	private boolean enabled = Boolean.TRUE;
	@PositiveOrZero private Integer quantity = 1;
	@PositiveOrZero private Double percentageStopLoss = 25D;
	@NotBlank private String positionOpenCron = "0 16 9 * * MON-FRI";
	@NotBlank private String positionCloseCron = "0 15 15 * * MON-FRI";
	private final String id = Index.NIFTY50.getCode() + "-" + TradingStrategyTemplate.SHORT_STRADDLE.getId();
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;
	
}
