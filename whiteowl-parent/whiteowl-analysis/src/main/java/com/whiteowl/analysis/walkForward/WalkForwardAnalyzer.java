package com.whiteowl.analysis.walkForward;

import java.util.List;
import java.util.Set;

import org.ta4j.core.BarSeries;

import com.whiteowl.analysis.backtester.Backtest;
import com.whiteowl.analysis.backtester.listener.AccumulatorBacktestListener;
import com.whiteowl.analysis.optimizer.TradingStrategyConfigOptimizationListener;
import com.whiteowl.analysis.optimizer.TradingStrategyConfigOptimizer;
import com.whiteowl.analysis.optimizer.TradingStrategyConfigOptimizerConfig;
import com.whiteowl.analysis.performance.TradingStrategyConfigPerformance;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WalkForwardAnalyzer {

	private final BarService barService;
	private final TradingStrategyConfigOptimizer optimizer;
	
	public void walk(WalkForwardConfig walkForwardConfig, WalkForwardListener listener) {
		final Timeframe timeframe = walkForwardConfig.getTimeframe();
		final double initialMargin = walkForwardConfig.getInitialMargin(), slippagePercentage = walkForwardConfig.getSlippagePercentage();
		final int testBarCount = walkForwardConfig.getTestBarCount(), trainBarCount = walkForwardConfig.getTrainBarCount(), steps = walkForwardConfig.getSteps();
		
		listener.onStart(walkForwardConfig);
		
		for(int stepIndex = 0; stepIndex < steps; stepIndex++) {
			
			listener.onTrainStart(stepIndex);
			final TradingStrategyConfig selectedConfig = optimizer.optimize(TradingStrategyConfigOptimizerConfig.builder()
					.timeframe(timeframe)
					.scrips(walkForwardConfig.getScrips())
					.configs(walkForwardConfig.getConfigs())
					.offsetPeriod((steps - stepIndex) * testBarCount)
					.initialMargin(initialMargin)
					.lookbackPeriod(trainBarCount)
					.slippagePercentage(slippagePercentage)
					.optimisationFunction(TradingStrategyConfigPerformance::getCagrOverAvgDrawdown)
					.build(), TradingStrategyConfigOptimizationListener.NULL);
			listener.onTrainEnd(stepIndex, selectedConfig);
			
			listener.onTestStart(stepIndex, selectedConfig);
			final int stepIndex_ = stepIndex;
			final AccumulatorBacktestListener testBacktestListener = new AccumulatorBacktestListener();
			final AccumulatorBacktestListener trainBacktestListener = new AccumulatorBacktestListener();
			walkForwardConfig.getScrips().parallelStream().forEach(scrip -> {
				final int requiredBarCount = selectedConfig.getMinBarCount() + trainBarCount + testBarCount;
				final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
						scrip.getCode(), timeframe, requiredBarCount, (steps - stepIndex_ - 1) * testBarCount);
				if(barSeries.getBarCount() < requiredBarCount) {
					log.warn("Insufficient bars, required {}, but got only {}, scrip {} will be skipped", 
							requiredBarCount, barSeries.getBarCount(), scrip.getCode());
					return;
				}
				Backtest.builder()
					.scrip(scrip)
					.timeframe(timeframe)
					.config(selectedConfig)
					.bars(barSeries.getBarData().subList(0, requiredBarCount - testBarCount))
					.initialMargin(initialMargin)
					.slippagePercentage(slippagePercentage)
					.build()
					.execute(trainBacktestListener);
				Backtest.builder()
					.scrip(scrip)
					.timeframe(timeframe)
					.config(selectedConfig)
					.bars(barSeries.getBarData().subList(requiredBarCount - trainBarCount, requiredBarCount))
					.initialMargin(initialMargin)
					.slippagePercentage(slippagePercentage)
					.build()
					.execute(testBacktestListener);
			});
			final TradingStrategyConfigPerformance testPerformance = new TradingStrategyConfigPerformance(initialMargin, testBacktestListener.getPositions());
			final TradingStrategyConfigPerformance trainPerformance = new TradingStrategyConfigPerformance(initialMargin, trainBacktestListener.getPositions());
			listener.onTestEnd(WalkForwardStep.builder()
					.selectedConfig(selectedConfig)
					.testBarCount(testBarCount)
					.testPerformance(testPerformance)
					.trainBarCount(trainBarCount)
					.trainPerformance(trainPerformance)
					.build());
		} 
		
		listener.onEnd();
	}
	
	public static void main(String[] args) {
		final JdbcBarRepository barRepository = JdbcBarRepository.instance();
		final BarService barService = new DefaultBarService(barRepository);
		final ScripService scripService = JdbcScripService.instance();
		final TradingStrategyConfigOptimizer optimisationService = new TradingStrategyConfigOptimizer(barService);
		final WalkForwardAnalyzer walkForwardService = new WalkForwardAnalyzer(barService, optimisationService);
		
		final int steps = 8;
		final double initialMargin = 10_00_000;
		final double slippagePercentage = 0.5;
		final Timeframe timeframe = Timeframe.M15;
		final List<Scrip> scrips = scripService.findByIndices(Index.NIFTY50).subList(0, 5);
		final Set<TradingStrategyConfig> configs = new MACDLongTradingStrategyConfig(0, 0, 0).getOptimisationUniverse();
		final int trainBarCount = 500 * timeframe.getDayMultiple();
		final int testBarCount = 66 * timeframe.getDayMultiple();
		
		final WalkForwardConfig walkForwardConfig = WalkForwardConfig.builder()
				.steps(steps)
				.scrips(scrips)
				.configs(configs)
				.timeframe(timeframe)
				.testBarCount(testBarCount)
				.trainBarCount(trainBarCount)
				.initialMargin(initialMargin)
				.slippagePercentage(slippagePercentage)
				.build();
		final WalkForwardReport walkForwardReport = new WalkForwardReport(initialMargin);
		walkForwardService.walk(walkForwardConfig, walkForwardReport);
		System.out.println(walkForwardReport);
	}
	
	
}
