package com.whiteowl.analysis.performance;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyConfigPerformanceService implements TradingStrategyConfigPerformanceService {

	@Delegate private final TradingStrategyConfigPerformanceRepository repository;

}
