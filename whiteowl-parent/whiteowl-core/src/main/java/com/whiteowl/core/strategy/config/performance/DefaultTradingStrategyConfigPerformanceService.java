package com.whiteowl.core.strategy.config.performance;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyConfigPerformanceService implements TradingStrategyConfigPerformanceService {

	@Delegate private final TradingStrategyConfigPerformanceRepository repository;

}
