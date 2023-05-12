package com.whiteowl.core.strategy.config;

import java.util.Set;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

public interface TradingStrategyConfig {
	
	String getId();
	
	int getMinBarCount();
	
	String getScripCode();
	
	TradeType getTradeType();
	
	Timeframe getTimeframe();
	
	Set<TradingStrategyConfig> getNeighbours();
	
	Set<TradingStrategyConfig> getOptimisationUniverse();
	
	TradingStrategy createTradingStrategy(TradingStrategyContext context);
	
}
