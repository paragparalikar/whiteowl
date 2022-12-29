package com.whiteowl.strategy.config;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Service
@Validated
@RequiredArgsConstructor
public class DefaultTradingStrategyConfigService implements TradingStrategyConfigService {

	@Delegate
	private final TradingStrategyConfigRepository tradingStrategyConfigRepository;
	
}
