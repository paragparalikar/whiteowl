package com.whiteowl.strategy.test.mock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockTradingStrategyConfigService implements TradingStrategyConfigService {

	@NonNull private final TradingStrategyConfig config;
	
	@Override
	public TradingStrategyConfig save(@NonNull TradingStrategyConfig config) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<TradingStrategyConfig> findAll() {
		return Collections.singletonList(config);
	}

	@Override
	public Optional<TradingStrategyConfig> findById(@NonNull String id) {
		return id.equals(config.getId()) ? Optional.of(config) : Optional.empty();
	}

	@Override
	public List<TradingStrategyConfig> findByTemplate(@NonNull TradingStrategyTemplate template) {
		return template.equals(config.getTradingStrategyTemplate()) ?
				Collections.singletonList(config) : Collections.emptyList();
	}

	@Override
	public List<TradingStrategyConfig> findByScripCodeAndTimeframe(@NonNull String scripCode,
			@NonNull Timeframe timeframe) {
		return Optional.of(config)
				.filter(config -> scripCode.equals(config.getScripCode()))
				.filter(config -> timeframe.equals(config.getTimeframe()))
				.map(Collections::singletonList)
				.orElse(Collections.emptyList());
	}

	@Override
	public Optional<TradingStrategyConfig> findByScripCodeAndTimeframeAndTemplate(@NonNull String scripCode,
			@NonNull Timeframe timeframe, @NonNull TradingStrategyTemplate template) {
		return Optional.of(config)
				.filter(config -> scripCode.equals(config.getScripCode()))
				.filter(config -> timeframe.equals(config.getTimeframe()))
				.filter(config -> template.equals(config.getTradingStrategyTemplate()));
	}

}
