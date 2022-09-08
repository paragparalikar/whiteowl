package com.whiteowl.job;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.whiteowl.job.TradeSynchronizationJob.TradesSynchronizedEvent;
import com.whiteowl.strategy.TradingStrategyService;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradingStrategyExecutionJob {

	private final TradingStrategyService tradingStrategyService;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	
	@EventListener(TradesSynchronizedEvent.class)
	public void tryExecute() {
		tradingStrategyConfigService.findByEnabled(true).stream()
			.forEach(tradingStrategyService::execute);
	}

}
