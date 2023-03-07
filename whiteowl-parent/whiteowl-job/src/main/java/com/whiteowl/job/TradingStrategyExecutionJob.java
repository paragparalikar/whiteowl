package com.whiteowl.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.TradingStrategyConfigService;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(JobConstant.ORDER_TRADING_STRATEGY_EXECUTION)
public class TradingStrategyExecutionJob implements CommandLineRunner, AutoCloseable {
	
	private final TradingStrategyContext context;
	private final TradingStrategyConfigService configService;
	private final Map<TradingStrategyConfig, TradingStrategy> cache = new ConcurrentHashMap<>();

	@Override
	public void run(String... args) throws Exception {
		for(TradingStrategyConfig config : configService.findAll()) {
			cache.put(config, config.createTradingStrategy(context));
		}
	}

	@Override
	public void close() throws Exception {
		for(TradingStrategy strategy : cache.values()) strategy.close();
	}

}
