package com.whiteowl.core.strategy.test;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.NonNull;

public interface BackTestService {

	BackTestResult test(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull TradingStrategyConfig config);

}
