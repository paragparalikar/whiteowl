package com.whiteowl.core.analysis.optimiser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.DefaultBarService;
import com.whiteowl.core.bar.JdbcBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.impl.MACDLongTradingStrategyConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WalkForwardService {

	private final BarService barService;
	private final OptimisationService optimisationService;
	
	public TradingStrategyConfigPerformance walk(
			List<Scrip> scrips, Timeframe timeframe, Set<TradingStrategyConfig> configs,
			int trainBarCount, int testBarCount, int steps, double initialMargin, double slippagePercentage) {
		final List<Position> positions = new ArrayList<>();
		final Function<TradingStrategyConfigPerformance, Double> optimisationFunction = TradingStrategyConfigPerformance::getCagrOverAvgDrawdown;
		for(int index = testBarCount * steps; index > 0; index -= testBarCount) {
			final int index_ = index;
			final TradingStrategyConfig optimumConfig = optimisationService.optimise(scrips, timeframe, configs, optimisationFunction, 
					index, trainBarCount, initialMargin, slippagePercentage);
			scrips.parallelStream().forEach(scrip -> {
				final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
						scrip.getCode(), timeframe, testBarCount, index_ + testBarCount);
				Backtest.builder()
						.scrip(scrip)
						.timeframe(timeframe)
						.bars(barSeries.getBarData())
						.initialMargin(initialMargin)
						.slippagePercentage(slippagePercentage)
						.build()
						.execute(optimumConfig)
						.forEach(positions::add);
			});
		}
		return new TradingStrategyConfigPerformance(initialMargin, positions);
	}
	
	public static void main(String[] args) {
		final JdbcBarRepository barRepository = JdbcBarRepository.instance();
		final BarService barService = new DefaultBarService(barRepository);
		final OptimisationService optimisationService = new OptimisationService(barService);
		final WalkForwardService walkForwardService = new WalkForwardService(barService, optimisationService);
		
		final Timeframe timeframe = Timeframe.M15;
		final List<String> codes = barRepository.findAllCodes();
		final Set<TradingStrategyConfig> configs = new MACDLongTradingStrategyConfig(null, null, 0, 0, 0).getOptimisationUniverse();
		
	}
	
	
}
