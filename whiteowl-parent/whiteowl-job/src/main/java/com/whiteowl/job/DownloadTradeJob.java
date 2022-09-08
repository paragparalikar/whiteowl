package com.whiteowl.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.trade.TradeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DownloadTradeJob {
	
	private final TradeService tradeService;
	private final PortfolioService portfolioService;
	private final BrokerServiceProvider brokerServiceProvider;

	@Scheduled(cron = "0/15 0 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		for(Portfolio portfolio : portfolioService.findAll()) {
			brokerServiceProvider.findAllTrades(portfolio)
				.forEach(tradeService::save);
		}
	}
	
}
