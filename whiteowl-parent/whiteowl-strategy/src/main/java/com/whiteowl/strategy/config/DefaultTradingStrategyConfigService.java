package com.whiteowl.strategy.config;

import java.util.List;

import javax.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.whiteowl.strategy.TradingStrategyConfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class DefaultTradingStrategyConfigService implements TradingStrategyConfigService {

	private final TradingStrategyConfigRepository tradingStrategyConfigRepository;
	
	@Override
	public long count() {
		return tradingStrategyConfigRepository.count();
	}

	@Override
	public TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config) {
		final PersistentTradingStrategyConfig persistentConfig = new PersistentTradingStrategyConfig(config);
		tradingStrategyConfigRepository.save(persistentConfig);
		return persistentConfig.getDelegate();
	}

	@Override
	public List<TradingStrategyConfig> findAll() {
		return tradingStrategyConfigRepository.findAll().stream()
				.map(PersistentTradingStrategyConfig::getDelegate)
				.toList();
	}

	@Override
	public List<TradingStrategyConfig> findByEnabled(boolean value) {
		return tradingStrategyConfigRepository.findByEnabled(value).stream()
				.map(PersistentTradingStrategyConfig::getDelegate)
				.toList();
	}

	@Override
	public void deleteById(@NonNull Long id) {
		tradingStrategyConfigRepository.deleteById(id);
	}

}
