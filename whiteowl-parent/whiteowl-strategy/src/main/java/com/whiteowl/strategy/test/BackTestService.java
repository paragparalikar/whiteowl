package com.whiteowl.strategy.test;

import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface BackTestService {

	BackTestResult test(@NonNull TradingStrategyConfig config);

}
