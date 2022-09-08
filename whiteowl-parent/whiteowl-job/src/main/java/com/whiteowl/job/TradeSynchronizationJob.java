package com.whiteowl.job;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.trade.Trade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeSynchronizationJob {
	
	public static class TradesSynchronizedEvent{}
	
	private final PositionService positionService;
	private final PortfolioService portfolioService;
	private final ApplicationEventPublisher eventPublisher;
	private final BrokerServiceProvider brokerServiceProvider;

	@Scheduled(cron = "0/15 0 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		for(Portfolio portfolio : portfolioService.findAll()) {
			final Map<String, Trade> trades = brokerServiceProvider.findAllTrades(portfolio).stream()
					.collect(Collectors.toMap(Trade::getBrokerTradeId, Function.identity()));
			final List<Position> positions = positionService.findByPortfolioAndStatusNot(portfolio, PositionStatus.CLOSED);
			for(Position position : positions) {
				sync(position.getExitTrades(), trades);
				sync(position.getEntryTrades(), trades);
				position.updateStatus();
				positionService.save(position);
			}
		}
		eventPublisher.publishEvent(new TradesSynchronizedEvent());
	}
	
	private void sync(Set<Trade> localTrades, Map<String, Trade> brokerTrades) {
		localTrades.forEach(localTrade -> 
			Optional.ofNullable(brokerTrades.get(localTrade.getBrokerTradeId()))
				.ifPresent(brokerTrade -> copy(brokerTrade, localTrade)));
	}
	
	private void copy(Trade from, Trade to) {
		to.setStatus(from.getStatus());
		to.setTimestamp(from.getTimestamp());
		to.setAveragePrice(from.getAveragePrice());
		to.setFilledQuantity(from.getFilledQuantity());
		to.setPendingQuantity(from.getPendingQuantity());
		to.setExchangeTradeId(from.getExchangeTradeId());
		to.setExchangeTimestamp(from.getExchangeTimestamp());
	}
	
}
