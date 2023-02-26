package com.whiteowl.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeNotFoundException;
import com.whiteowl.core.trade.TradeSynchronizedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeSynchronizationJob implements AutoCloseable {
	
	private final PositionService positionService;
	private final PortfolioService portfolioService;
	private final ApplicationEventPublisher eventPublisher;
	private final BrokerServiceProvider brokerServiceProvider;
	private final Map<Portfolio, Consumer<Trade>> tradeListeners = new HashMap<>();
	
	@Scheduled(cron = "0 * 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		try {
			for(Portfolio portfolio : portfolioService.findAll()) {
				brokerServiceProvider
					.findAllTrades(portfolio)
					.forEach(this::synchronize);
			}
		} catch(Exception e) {
			log.error("", e);
		}
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void subscribe() {
		for(Portfolio portfolio : portfolioService.findAll()) {
			final Consumer<Trade> tradeListener = this::synchronize;
			brokerServiceProvider.subscribeTradeStatusListener(tradeListener, portfolio);
			tradeListeners.put(portfolio, tradeListener);
		}
	}
	
	private void synchronize(Trade trade) {
		final String brokerTradeId = trade.getBrokerTradeId();
		Optional<Position> optionalPosition = positionService.findByEntryTradesBrokerTradeId(brokerTradeId);
		if(optionalPosition.isEmpty()) optionalPosition = positionService.findByExitTradesBrokerTradeId(brokerTradeId);
		optionalPosition.ifPresent(position -> {
			final Trade localTrade = Stream.concat(position.getEntryTrades().stream(), position.getExitTrades().stream())
				.filter(lt -> Objects.equals(lt.getId(), trade.getId())).findFirst()
				.orElseThrow(() -> new TradeNotFoundException("No trade found for id " + trade.getId()));
			localTrade.copy(trade);
			positionService.save(position);
			eventPublisher.publishEvent(new TradeSynchronizedEvent(localTrade, position));
		});
	}
	
	@Override
	public void close() throws Exception {
		for(Portfolio portfolio : tradeListeners.keySet()) {
			final Consumer<Trade> tradeListener = tradeListeners.get(portfolio);
			brokerServiceProvider.unsubscribeTradeStatusListener(tradeListener, portfolio);
		}
		tradeListeners.clear();
	}
	
}
