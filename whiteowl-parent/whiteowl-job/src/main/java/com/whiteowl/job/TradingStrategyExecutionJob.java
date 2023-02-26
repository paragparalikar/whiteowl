package com.whiteowl.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.strategy.TradingStrategyExecutor;
import com.whiteowl.core.trade.TradeSynchronizedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradingStrategyExecutionJob {

	private final TradingStrategyExecutor tradingStrategyExecutor;
	
	@EventListener
	public void handle(final ApplicationReadyEvent event) {
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
