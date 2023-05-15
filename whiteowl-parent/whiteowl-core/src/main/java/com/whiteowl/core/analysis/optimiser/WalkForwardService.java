package com.whiteowl.core.analysis.optimiser;

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
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.JdbcScripService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
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
		final TradingStrategyConfigPerformance performance = new TradingStrategyConfigPerformance(initialMargin);
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
						.execute(optimumConfig, performance);
			});
		}
		return performance;
	}
	
	public static void main(String[] args) {
		final JdbcBarRepository barRepository = JdbcBarRepository.instance();
		final BarService barService = new DefaultBarService(barRepository);
		final ScripService scripService = JdbcScripService.instance();
		final OptimisationService optimisationService = new OptimisationService(barService);
		final WalkForwardService walkForwardService = new WalkForwardService(barService, optimisationService);
		
		final int steps = 8;
		final double initialMargin = 10_00_000;
		final double slippagePercentage = 0.5;
		final Timeframe timeframe = Timeframe.M15;
		final List<Scrip> scrips = scripService.findByIndices(Index.NIFTY50);
		final Set<TradingStrategyConfig> configs = new MACDLongTradingStrategyConfig(0, 0, 0).getOptimisationUniverse();
		final int trainBarCount = 500 * timeframe.getDayMultiple();
		final int testBarCount = 66 * timeframe.getDayMultiple();
		
		final TradingStrategyConfigPerformance performance = walkForwardService.walk(scrips, timeframe, 
				configs, trainBarCount, testBarCount, steps, initialMargin, slippagePercentage);
		System.out.println(performance);
	}
	
	
}
