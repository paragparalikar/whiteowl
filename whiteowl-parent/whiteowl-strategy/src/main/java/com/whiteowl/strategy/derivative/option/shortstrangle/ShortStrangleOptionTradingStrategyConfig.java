package com.whiteowl.strategy.derivative.option.shortstrangle;

import com.whiteowl.strategy.TradingStrategyConstants;
import com.whiteowl.strategy.TradingStrategyType;
import com.whiteowl.strategy.derivative.option.OptionTradingStrategyConfig;

import lombok.Data;

@Data
public class ShortStrangleOptionTradingStrategyConfig implements OptionTradingStrategyConfig {

	private String id;
	private final TradingStrategyType tradingStrategyType = TradingStrategyType.OPTION;
	private final String tradingStrategyId = TradingStrategyConstants.ID_SHORT_STRANGLE;

	
	
}
