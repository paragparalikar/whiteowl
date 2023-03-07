package com.whiteowl.core.strategy.context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.trade.Trade;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Component
@RequiredArgsConstructor
public class DefaultTradingStrategyContext implements TradingStrategyContext {

	private final ScripService scripService;
	private final QuoteService quoteService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	private final PortfolioService portfolioService;
	@Delegate private final BarSeriesCacheManager barListenerManager;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	
	@Override
	public ScheduledFuture<?> schedule(Runnable runnable, String cronExpression) {
		final CronTrigger cronTrigger = new CronTrigger(cronExpression);
		return taskScheduler.schedule(runnable, cronTrigger);
	}
	
	@Override
	public void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		quoteService.subscribe(scrips, mode, quoteListener);
	}
	
	@Override
	public void unsubscribe(Consumer<Quote> quoteListener) {
		quoteService.unsubscribe(quoteListener);
	}
	
	@Override
	public Scrip getScrip(String scripCode) {
		return scripService.findByCode(scripCode);
	}
	
	@Override
	public List<Position> getOpenPositions(String configId) {
		return positionService.findByTradingStrategyConfigIdAndStatusNot(configId, PositionStatus.CLOSED);
	}
	
	@Override
	public void save(Position position) {
		if(null == position.getPortfolio()) {
			portfolioService.findAll().stream()
				.map(position::withPortfolio)
				.map(this::dispatch)
				.forEach(positionService::save);
		} else {
			positionService.save(dispatch(position));
		}
	}

	private Position dispatch(Position position) {
		position.getEntryTrades().forEach(trade -> dispatch(trade, position.getPortfolio()));
		position.getExitTrades().forEach(trade -> dispatch(trade, position.getPortfolio()));
		PositionStatus.update(position);
		return position;
	}
	
	private void dispatch(Trade trade, Portfolio portfolio) {
		if(trade.getStatus().isActionable()) {
			final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
					.getBrokerServiceProvider(portfolio.getBroker());
			switch(trade.getStatus()) {
			case NEW: brokerServiceProvider.create(trade, portfolio); break;
			case UPDATABLE: brokerServiceProvider.update(trade, portfolio); break;
			case CANCELLABLE: brokerServiceProvider.cancel(trade, portfolio); break;
			default: break;
			}
		}
	}
	
}
