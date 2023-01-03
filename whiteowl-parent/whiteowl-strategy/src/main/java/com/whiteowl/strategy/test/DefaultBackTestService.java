package com.whiteowl.strategy.test;

import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.bar.BarRepository;
import com.whiteowl.core.bar.FileSystemBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.impl.trendfollowing.TrendFollowingTradingStrategyConfig;
import com.whiteowl.strategy.performance.DefaultTradingStrategyPerformanceService;
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
	
	public static void main(String[] args) {
		final Timeframe timeframe = Timeframe.M15;
		final Scrip scrip = Scrip.builder()
				.code("RELIANCE")
				.type(ScripType.EQ)
				.exchange(Exchange.NSE)
				.build();
		final BarRepository barRepository = new FileSystemBarRepository();
		final List<Bar> bars = barRepository.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		final BarSeries barSeries = new BaseBarSeries("", bars, DoubleNum::valueOf);
		final TradingStrategyConfig config = new TrendFollowingTradingStrategyConfig(scrip.getCode());
		final TradingStrategyPerformanceService performanceService = new DefaultTradingStrategyPerformanceService(null);
		final BackTestService backTestService = new DefaultBackTestService(performanceService);
		final BackTestResult result = backTestService.test(scrip, barSeries, config);
		System.out.println(result);
	}
	
}
