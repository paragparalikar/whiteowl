package com.whiteowl.core.strategy.context;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Component
@RequiredArgsConstructor
public class DefaultTradingStrategyContext implements TradingStrategyContext {

	private final QuoteService quoteService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	@Delegate private final BarSeriesCacheManager barListenerManager;
	
	public ScheduledFuture<?> schedule(Runnable runnable, String cronExpression){
		final CronTrigger cronTrigger = new CronTrigger(cronExpression);
		return taskScheduler.schedule(runnable, cronTrigger);
	}
	
	public void subscribeQuoteListener(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		quoteService.subscribe(scrips, mode, quoteListener);
	}
	
	public void unsubscribeQuoteListener(Consumer<Quote> quoteListener) {
		quoteService.unsubscribe(quoteListener);
	}
	
	public Position save(Position position) {
		return positionService.save(position);
	}

}
