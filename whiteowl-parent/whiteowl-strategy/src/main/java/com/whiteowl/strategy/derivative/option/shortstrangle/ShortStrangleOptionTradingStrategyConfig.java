package com.whiteowl.strategy.derivative.option.shortstrangle;

import java.time.LocalTime;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.strategy.TradingStrategyConstants;
import com.whiteowl.strategy.TradingStrategyType;
import com.whiteowl.strategy.derivative.option.OptionTradingStrategyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortStrangleOptionTradingStrategyConfig implements OptionTradingStrategyConfig {

	private String id;
	private final TradingStrategyType tradingStrategyType = TradingStrategyType.OPTION;
	private final String tradingStrategyId = TradingStrategyConstants.ID_SHORT_STRANGLE;
	private final ScripCriteria scripCriteria = new ScripCriteria().withCode(Index.NIFTY50.getCode());
	
	private LocalTime startTime = LocalTime.of(9, 20);
	private LocalTime stopTime = LocalTime.of(15, 25);
	
	private Double callDelta = 0.5D;
	private Integer callQuantity = 1;
	private Double callPercentageTarget = 40D;
	private Double callPercentageStopLoss = 20D;
	
	private Double putDelta = 0.5D;
	private Integer putQuantity = 1;
	private Double putPercentageTarget = 40D;
	private Double putPercentageStopLoss = 20D;
	
}
