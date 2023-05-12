package com.whiteowl.core.analysis.optimiser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.analysis.backtester.BackTestService;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.DefaultBarService;
import com.whiteowl.core.bar.JdbcBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.impl.MACDLongTradingStrategyConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WalkForwardService {

	private final BarService barService;
	private final BackTestService backTestService;
	private final OptimisationService optimisationService;
	
	public TradingStrategyConfigPerformance walk(
			List<String> codes, Timeframe timeframe, Set<TradingStrategyConfig> configs,
			int trainBarCount, int testBarCount, int steps, double initialMargin, double slippagePercentage) {
		final List<Position> positions = new ArrayList<>();
		final OptimisationFunction optimisationFunction = TradingStrategyConfigPerformance::getCagrOverAvgDrawdown;
		for(int index = testBarCount * steps; index > 0; index -= testBarCount) {
			final TradingStrategyConfig optimumConfig = optimisationService.optimise(codes, timeframe, configs, optimisationFunction, 
					index, trainBarCount, initialMargin, slippagePercentage);
			for(String code : codes) {
				final List<Bar> bars = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
						code, timeframe, testBarCount, index + testBarCount).getBarData();
				final List<Position> testPositions = backTestService.backtest(optimumConfig, bars, initialMargin, slippagePercentage);
				positions.addAll(testPositions);
			}
		}
		return new TradingStrategyConfigPerformance(initialMargin, positions);
	}
	
	public static void main(String[] args) {
		final JdbcBarRepository barRepository = JdbcBarRepository.instance();
		final BarService barService = new DefaultBarService(barRepository);
		final BackTestService backTestService = new BackTestService();
		final OptimisationService optimisationService = new OptimisationService(barService, backTestService);
		final WalkForwardService walkForwardService = new WalkForwardService(barService, backTestService, optimisationService);
		
		final Timeframe timeframe = Timeframe.M15;
		final List<String> codes = barRepository.findAllCodes();
		final Set<TradingStrategyConfig> configs = new MACDLongTradingStrategyConfig(null, null, 0, 0, 0).getOptimisationUniverse();
		
	}
	
	
}
