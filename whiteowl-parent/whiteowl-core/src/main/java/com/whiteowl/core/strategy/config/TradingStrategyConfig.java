package com.whiteowl.core.strategy.config;

import java.io.Serializable;
import java.util.Set;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

public interface TradingStrategyConfig extends Serializable{
	
	String getId();
	
	int getMinBarCount();
	
	TradeType getTradeType();
	
	Set<TradingStrategyConfig> getNeighbours();
	
	Set<TradingStrategyConfig> getOptimisationUniverse();
	
	TradingStrategy createTradingStrategy(Scrip scrip, Timeframe timeframe, TradingStrategyContext context);
	
}
