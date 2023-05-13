package com.whiteowl.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.TradingStrategyConfigService;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(JobConstant.ORDER_TRADING_STRATEGY_EXECUTION)
public class TradingStrategyExecutionJob implements CommandLineRunner, AutoCloseable {
	
	private final ScripService scripService;
	private final TradingStrategyContext context;
	private final TradingStrategyConfigService configService;
	private final Map<TradingStrategyConfig, TradingStrategy> cache = new ConcurrentHashMap<>();

	@Override
	public void run(String... args) throws Exception {
		// TODO There should be a service that knows the mapping between scrip-timeframe and configs
		for(Scrip scrip : scripService.findByIndices(Index.NIFTY50)) {
			for(TradingStrategyConfig config : configService.findAll()) {
				cache.put(config, config.createTradingStrategy(scrip, Timeframe.M15, context));
			}
		}
	}

	@Override
	public void close() throws Exception {
		for(TradingStrategy strategy : cache.values()) strategy.close();
	}

}
