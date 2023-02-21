package com.whiteowl.core.strategy.config;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.strategy.TradingStrategyTemplate;

import lombok.NonNull;

public interface TradingStrategyConfigRepository {
	
	List<TradingStrategyConfig> findAll();
	
	Optional<TradingStrategyConfig> findById(
			@NonNull final String id);
	
	TradingStrategyConfig save(
			@NonNull @Valid final TradingStrategyConfig config);

	List<TradingStrategyConfig> findByTemplate(
			@NonNull final TradingStrategyTemplate template);
	
	List<TradingStrategyConfig> findByScripCodeAndTimeframe(
			@NonNull final String scripCode,
			@NonNull final Timeframe timeframe);
	
	Optional<TradingStrategyConfig> findByScripCodeAndTimeframeAndTemplate(
			@NonNull final String scripCode,
			@NonNull final Timeframe timeframe,
			@NonNull final TradingStrategyTemplate template);
	
}
