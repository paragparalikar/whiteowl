package com.whiteowl.job;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.strategy.TradingStrategyExecutor;
import com.whiteowl.core.trade.TradeSynchronizedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(JobConstant.ORDER_TRADING_STRATEGY_EXECUTION)
public class TradingStrategyExecutionJob implements CommandLineRunner {

	private final TradingStrategyExecutor tradingStrategyExecutor;
	
	@Override
	public void run(String... args) throws Exception {
		tradingStrategyExecutor.onApplicationReady();
	}
	
	@EventListener
	public void handle(final ScripBarDownloadedEvent event) {
		tradingStrategyExecutor.onScripBarDownloaded(event.getScrip(), event.getTimeframe());
	}
	
	@EventListener
	public void handle(final TradeSynchronizedEvent event) {
		tradingStrategyExecutor.onTradeSynchronized(
				event.getTrade(),
				event.getPosition());
	}
	
}
