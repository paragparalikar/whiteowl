package com.whiteowl.strategy.test;

import java.util.List;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.performance.TradingStrategyPerformance;
import com.whiteowl.strategy.performance.TradingStrategyPerformanceService;
import com.whiteowl.strategy.test.mock.MockContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	@NonNull private final TradingStrategyPerformanceService tradingStrategyPerformaceService;
	
	@Override
	public BackTestResult test(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull final TradingStrategyConfig config) {
		final MockContext context = new MockContext(scrip, barSeries, config);
		context.getBackTestExecutor().execute();
		final List<Position> positions = context.getPositionService().findAll();
		final List<Double> equityCurve = context.getEquityCurveObserver().getEquityCurve();
		final TradingStrategyPerformance performance = tradingStrategyPerformaceService
				.calculate(barSeries.getBarData(), positions, equityCurve, config);
		return BackTestResult.builder()
				.scrip(scrip)
				.config(config)
				.timeframe(config.getTimeframe())
				.positions(positions)
				.performance(performance)
				.build();
	}
	
}
