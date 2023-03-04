package com.whiteowl.core.strategy.context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Component
@RequiredArgsConstructor
public class DefaultTradingStrategyContext implements TradingStrategyContext {

	private final ScripService scripService;
	private final QuoteService quoteService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	@Delegate private final BarSeriesCacheManager barListenerManager;
	
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
	public Position save(Position position) {
		return positionService.save(position);
	}

	@Override
	public List<Position> getOpenPositions(String configId, PositionStatus status) {
		return positionService.findByTradingStrategyConfigIdAndStatusNot(configId, status);
	}
	
}
