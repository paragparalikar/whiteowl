package com.whiteowl.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.trade.TradeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeSynchronizationJob {
	
	public static class TradesSynchronizedEvent{}
	
	private final TradeService tradeService;
	private final PortfolioService portfolioService;
	private final ApplicationEventPublisher eventPublisher;
	private final BrokerServiceProvider brokerServiceProvider;

	@Scheduled(cron = "0/15 0 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		portfolioService.findAll().stream()
			.flatMap(portfolio -> brokerServiceProvider.findAllTrades(portfolio).stream())
			.forEach(tradeService::save);
		eventPublisher.publishEvent(new TradesSynchronizedEvent());
	}
	
}
