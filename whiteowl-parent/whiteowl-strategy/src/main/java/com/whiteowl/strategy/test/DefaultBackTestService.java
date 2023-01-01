package com.whiteowl.strategy.test;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.performance.TradingStrategyPerformance;
import com.whiteowl.strategy.performance.TradingStrategyPerformanceService;
import com.whiteowl.strategy.test.mock.MockContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	private final BarService barService;
	private final ScripService scripService;
	private final TradingStrategyPerformanceService tradingStrategyPerformaceService;
	
	@Override
	public BackTestResult test(@NonNull final TradingStrategyConfig config) {
		final Timeframe timeframe = config.getTimeframe();
		final Scrip scrip = scripService.findByCode(config.getScripCode());
		final BarSeries barSeries = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		final MockContext context = new MockContext(scrip, barSeries, timeframe, config);
		final List<Double> equityCurve = new ArrayList<>(barSeries.getBarCount());
		final Portfolio portfolio = context.getPortfolio();
		equityCurve.add(portfolio.getAvailableMargin());
		while(context.next()) equityCurve.add(portfolio.getAvailableMargin());
		final List<Position> positions = context.getPositionService().findAll();
		final TradingStrategyPerformance performance = tradingStrategyPerformaceService
				.calculate(barSeries.getBarData(), positions, equityCurve, config);
		return BackTestResult.builder()
				.scrip(scrip)
				.config(config)
				.timeframe(timeframe)
				.positions(positions)
				.performance(performance)
				.build();
	}
	
}
