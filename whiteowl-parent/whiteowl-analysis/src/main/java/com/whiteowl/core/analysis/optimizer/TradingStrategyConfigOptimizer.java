package com.whiteowl.core.analysis.optimizer;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.analysis.backtester.Backtest;
import com.whiteowl.core.analysis.backtester.listener.AccumulatorBacktestListener;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.util.Tuple2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TradingStrategyConfigOptimizer {

	private final BarService barService;
	
	public TradingStrategyConfig optimize(
			TradingStrategyConfigOptimizerConfig optimizerConfig,
			TradingStrategyConfigOptimizationListener listener) {
		
		listener.onStart(optimizerConfig);
		
		final Map<TradingStrategyConfig, AccumulatorBacktestListener> configPositions = new ConcurrentHashMap<>();
		for(Scrip scrip : optimizerConfig.getScrips()) {
			
			final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
					scrip.getCode(), optimizerConfig.getTimeframe(), optimizerConfig.getLookbackPeriod(), optimizerConfig.getOffsetPeriod());
			
			optimizerConfig.getConfigs().parallelStream().forEach(config -> {
				Backtest.builder()
					.scrip(scrip)
					.timeframe(optimizerConfig.getTimeframe())
					.config(config)
					.bars(barSeries.getBarData())
					.initialMargin(optimizerConfig.getInitialMargin())
					.slippagePercentage(optimizerConfig.getSlippagePercentage())
					.build().execute(configPositions.computeIfAbsent(config, key -> new AccumulatorBacktestListener()));
			});
			
			log.info("Accumulated trades for scrip {}", scrip.getCode());
		}

		final TradingStrategyConfig selectedConfig = configPositions.entrySet().stream()
			.map(entry -> Tuple2.of(entry.getKey(), new TradingStrategyConfigPerformance(optimizerConfig.getInitialMargin(), entry.getValue().getPositions())))
			.max(Comparator.comparing(Tuple2::getValue, Comparator.comparing(optimizerConfig.getOptimisationFunction())))
			.map(Tuple2::getKey)
			.orElse(null);
		
		listener.onEnd(selectedConfig);
		
		return selectedConfig;
	}
	
}
