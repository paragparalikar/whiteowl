package com.whiteowl.job;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.position.event.PositionSynchronizedEvent;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.util.Tuple2;
import com.whiteowl.strategy.TradingStrategy;
import com.whiteowl.strategy.TradingStrategyFactory;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradingStrategyExecutionJob implements AutoCloseable {

	private final QuoteService quoteService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	private final TradingStrategyFactory tradingStrategyFactory;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	private final Collection<ScheduledFuture<?>> futures = new LinkedList<>();
	private final Map<Tuple2<Scrip, TradingStrategyConfig>, QuoteSubscription> subscriptions = new ConcurrentHashMap<>();
	
	@EventListener
	public void handle(final ApplicationReadyEvent event) {
		for(TradingStrategyConfig config : tradingStrategyConfigService.findAll()){
			Stream.of(config.getEntryCronExpression(), config.getExitCronExpression())
				.map(CronTrigger::new)
				.map(trigger -> taskScheduler.schedule(() -> execute(config), trigger))
				.forEach(futures::add);
		}
	}
	
	@Async
	@EventListener
	public void handle(final ScripBarDownloadedEvent event) {
		final Timeframe timeframe = event.getTimeframe();
		final String scripCode = event.getScrip().getCode();
		tradingStrategyConfigService
				.findByScripCodeAndTimeframe(scripCode, timeframe)
				.forEach(this::execute);
	}
	
	private void execute(TradingStrategyConfig config) {
		execute(config, null);
	}
	
	private void execute(TradingStrategyConfig config, Quote quote) {
		final List<Position> positions = positionService.findByTradingStrategyConfigIdAndStatusNot(
				config.getId(), PositionStatus.CLOSED);
		final TradingStrategy tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
		if(positions.isEmpty()) {
			tradingStrategy.enter()
				.ifPresent(position -> handle(position, config));
		} else if(null == quote) {
			positions.stream()
				.filter(tradingStrategy::manage)
				.forEach(position -> handle(position, config));
		} else {
			positions.stream()
				.filter(position -> tradingStrategy.manage(position, quote))
				.forEach(position -> handle(position, config));
		}
	}
	
	@Async
	@EventListener
	public void handle(final PositionSynchronizedEvent event) {
		final Position position = event.getPosition();
		final String configId = position.getTradingStrategyConfigId();
		final TradingStrategyConfig config = tradingStrategyConfigService.findById(configId).orElseThrow();
		final TradingStrategy tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
		if(tradingStrategy.manage(position)) handle(position, config);
	}
	
	private void handle(Position position, TradingStrategyConfig config) {
		position = positionService.save(position);
		if(position.getStatus().isTerminal()) unsubscribe(position, config);
		else subscribe(position, config);
	}
	
	private void subscribe(Position position, TradingStrategyConfig config) {
		for(Trade entryTrade : position.getEntryTrades()) {
			final Scrip scrip = entryTrade.getScrip();
			final Tuple2<Scrip, TradingStrategyConfig> key = Tuple2.of(scrip, config);
			if(!subscriptions.containsKey(key)) {
				final QuoteSubscription subscription = quoteService.subscribe(scrip);
				subscription.addListener(quote -> execute(config, quote));
				subscriptions.put(key, subscription);
			}
		}
	}
	
	private void unsubscribe(Position position, TradingStrategyConfig config) {
		position.getEntryTrades().stream()
			.map(Trade::getScrip)
			.map(scrip -> Tuple2.of(scrip, config))
			.forEach(subscriptions::remove);
	}
	
	@Override
	public void close() throws Exception {
		futures.forEach(future -> future.cancel(false));
		futures.clear();
		subscriptions.values().forEach(QuoteSubscription::unsubscribe);
		subscriptions.clear();
	}
	
}
