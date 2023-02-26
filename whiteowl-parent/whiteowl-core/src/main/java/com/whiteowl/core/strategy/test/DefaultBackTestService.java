package com.whiteowl.core.strategy.test;

import java.util.List;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.bar.JdbcBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.impl.trendfollowing.TrendFollowingTradingStrategyConfig;
import com.whiteowl.core.strategy.performance.DefaultTradingStrategyPerformanceService;
import com.whiteowl.core.strategy.performance.TradingStrategyPerformance;
import com.whiteowl.core.strategy.performance.TradingStrategyPerformanceService;
import com.whiteowl.core.strategy.test.mock.MockContext;

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
	
	public static void main(String[] args) throws Exception {
		final Timeframe timeframe = Timeframe.M15;
		final Scrip scrip = Scrip.builder()
				.code("RELIANCE")
				.type(ScripType.EQ)
				.exchange(Exchange.NSE)
				.build();
		final DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("sa");
		properties.setPassword("");
		properties.setDriverClassName("org.h2.Driver");
		properties.setUrl("jdbc:h2:~/.whiteowl/database/entities;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE");
		try(final JdbcBarRepository barRepository = new JdbcBarRepository(properties)){
			final List<Bar> bars = barRepository.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(scrip.getCode(), timeframe, Integer.MAX_VALUE);
			final BarSeries barSeries = new BaseBarSeries("", bars, DoubleNum::valueOf);
			final TradingStrategyConfig config = new TrendFollowingTradingStrategyConfig(scrip.getCode());
			final TradingStrategyPerformanceService performanceService = new DefaultTradingStrategyPerformanceService(null);
			final BackTestService backTestService = new DefaultBackTestService(performanceService);
			final BackTestResult result = backTestService.test(scrip, barSeries, config);
			System.out.println(result);
		}
	}
	
}
