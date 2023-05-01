package com.whiteowl.core.strategy.impl.trendfollowing;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Data;

@Data
public class MACDLongTradingStrategyConfig implements TradingStrategyConfig {

	private String scripCode;
	private Timeframe timeframe;
	private final TradeType tradeType = TradeType.BUY;
	private int longBarCount, shortBarCount, signalBarCount;
	
	@Override
	public int getMinBarCount() {
		return longBarCount + 1;
	}

	@Override
	public TradingStrategy createTradingStrategy(TradingStrategyContext context) {
		return new MACDLongTradingStrategy(this, context);
	}
	
	public String getId() {
		return String.join(java.io.File.separator, 
				getScripCode(), 
				getTimeframe().name(),
				String.valueOf(longBarCount),
				String.valueOf(shortBarCount),
				String.valueOf(signalBarCount));
	}

}
