package com.whiteowl.job;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.whiteowl.job.TradeSynchronizationJob.TradesSynchronizedEvent;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStrategyExecutionJob {

	private final TradingStrategyExecutor tradingStrategyExecutor;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	
	@EventListener(TradesSynchronizedEvent.class)
	public void tryExecute() {
		try {
			log.debug("Starting trading strategy execution job");
			tradingStrategyConfigService.findByEnabled(true).stream()
			.forEach(tradingStrategyExecutor::execute);
		} catch(Exception e) {
			log.error("", e);
		}
	}

}
