package com.whiteowl.strategy;

import javax.validation.Valid;

import org.springframework.scheduling.TaskScheduler;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface TradingStrategyExecutor<T extends TradingStrategyConfig> extends AutoCloseable {
	
	TradingStrategyTemplate getTradingStrategyTemplate();
	
	/**
	 * Invoked at the start of application. This method should be used to schedule time based strategy
	 * invocation, or trades on Daily timeframe. Basically anything that should be done only once in the 
	 * given trading day should be part of this method.
	 * 
	 * @param taskScheduler
	 */
	default void schedule(
			@Valid @NonNull final T config, 
			@NonNull final TaskScheduler taskScheduler) {};
	
	/**
	 * Invoked when data for all Scrips for provided timeframes has been downloaded and is up to date.
	 * 
	 * @param scrip
	 * @param config
	 */
	default void onTimeframeBarDataDownloaded(
			@Valid @NonNull final T config, 
			@NonNull final Timeframe timeframe) {};
	
	/**
	 * Invoked when data for provided Scrip and timeframe combination has been downloaded and is up to date.
	 * 
	 * @param config
	 * @param scrip
	 * @param timeframe
	 */
	default void onScripBarDataDownloaded(
			@Valid @NonNull final T config, 
			@NonNull final Scrip scrip, 
			@NonNull final Timeframe timeframe) {};
	
	/**
	 * Invoked when there is any change in position. This could be any trade being executed completely or partially,
	 * cancelled or any other change.
	 * 
	 * @param config
	 * @param position
	 */
	default void onPositionChanged(
			@Valid @NonNull final T config, 
			@NonNull final Position position) {};
	
}
