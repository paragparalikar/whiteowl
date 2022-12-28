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
	
	default void schedule(
			@Valid @NonNull final T config, 
			@NonNull final TaskScheduler taskScheduler) {};
	
	default void onTimeframeBarDataDownloaded(
			@Valid @NonNull final T config, 
			@NonNull final Timeframe timeframe) {};
	
	default void onScripBarDataDownloaded(
			@Valid @NonNull final T config, 
			@NonNull final Scrip scrip, 
			@NonNull final Timeframe timeframe) {};
	
	default void onPositionChanged(
			@Valid @NonNull final T config, 
			@NonNull final Position position) {};
	
}
