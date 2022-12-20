package com.whiteowl.strategy;

import org.springframework.scheduling.TaskScheduler;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.config.TradingStrategyConfig;

public interface TradingStrategyExecutor<T extends TradingStrategyConfig> {
	
	TradingStrategyTemplate getTradingStrategyTemplate();
	
	/**
	 * Invoked at the start of application. This method should be used to schedule time based strategy
	 * invocation, or trades on Daily timeframe. Basically anything that should be done only once in the 
	 * given trading day should be part of this method.
	 * 
	 * @param taskScheduler
	 */
	default void schedule(T config, TaskScheduler taskScheduler) {};
	
	/**
	 * Invoked when data for all Scrips for provided timeframes has been downloaded and is up to date.
	 * 
	 * @param scrip
	 * @param config
	 */
	default void onTimeframeBarDataDownloaded(T config, Timeframe timeframe) {};
	
	/**
	 * Invoked when data for provided Scrip and timeframe combination has been downloaded and is up to date.
	 * 
	 * @param config
	 * @param scrip
	 * @param timeframe
	 */
	default void onScripBarDataDownloaded(T config, Scrip scrip, Timeframe timeframe) {};
	
	/**
	 * Invoked when there is any change in position. This could be any trade being executed completely or partially,
	 * cancelled or any other change.
	 * 
	 * @param config
	 * @param position
	 */
	default void onPositionChanged(T config, Position position) {};
	
	/**
	 * Invoked when a quote is received from data provider. Processing should be kept minimal in this method as 
	 * it could be invoked quite rapidly.
	 * 
	 * @param config
	 * @param quote
	 */
	default void onQuoteDownloaded(T config, Position position, Quote quote) {};
	
}
