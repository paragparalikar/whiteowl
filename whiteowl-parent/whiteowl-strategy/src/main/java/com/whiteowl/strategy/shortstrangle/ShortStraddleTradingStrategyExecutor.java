package com.whiteowl.strategy.shortstrangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortStraddleTradingStrategyExecutor implements TradingStrategyExecutor<ShortStraddleConfig> {

	private final ScripService scripService;
	private final QuoteService quoteService;
	private final PositionService positionService;
	private final OptionChainService optionChainService;
	private final ShortStraddleTradingStrategy tradingStrategy = new ShortStraddleTradingStrategy();
	private final Map<ShortStraddleConfig, List<ScheduledFuture<?>>> futures = new ConcurrentHashMap<>();
	private final Map<ShortStraddleConfig, List<QuoteSubscription>> subscriptions = new ConcurrentHashMap<>();
	@Getter private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;
	
	@Override
	public void schedule(@NonNull final ShortStraddleConfig config, @NonNull final TaskScheduler taskScheduler) {
		if(log.isDebugEnabled()) log.debug("Scheduling config {}", config);
		final List<ScheduledFuture<?>> futures = this.futures.computeIfAbsent(config, key -> Collections.synchronizedList(new ArrayList<>(2)));
		futures.add(taskScheduler.schedule(() -> openPosition(config), new CronTrigger(config.getPositionOpenCron())));
		futures.add(taskScheduler.schedule(() -> closePosition(config), new CronTrigger(config.getPositionCloseCron())));
		positionService.findByTradingStrategyConfigIdAndStatusNot(config.getId(), PositionStatus.CLOSED)
			.forEach(position -> subscribe(position, config));
	}
	
	private void openPosition(ShortStraddleConfig config) {
		final Scrip scrip = scripService.findByCode(Index.NIFTY50.getCode());
		final OptionChain optionChain = optionChainService.findByScrip(scrip).orElseThrow();
		final Position position = tradingStrategy.openPosition(optionChain, config);
		subscribe(position, config);
		if(log.isInfoEnabled()) log.info("Opening position {} for config {}", position, config);
		positionService.save(position);
	}
	
	private void subscribe(Position position, ShortStraddleConfig config) {
		if(log.isDebugEnabled()) log.debug("Subscribing to quotes for position {}", position);
		position.getEntryTrades().stream()
			.map(Trade::getScrip)
			.map(tradeScrip -> quoteService.subscribe(tradeScrip, QuoteMode.LTP))
			.map(subscription -> subscription.addListener(quote -> onQuote(config, quote)))
			.forEach(subscriptions.computeIfAbsent(config, key -> Collections.synchronizedList(new ArrayList<>(2)))::add);
	}
	
	private void onQuote(ShortStraddleConfig config, Quote quote) {
		if(log.isDebugEnabled()) log.debug("Received quote {} for config {}", quote, config);
		positionService.findByTradingStrategyConfigIdAndStatusNot(config.getId(), PositionStatus.CLOSED).stream()
			.filter(position -> tradingStrategy.onQuote(position, quote, config))
			.forEach(position -> save(position, config));
	}
	
	private void unsubscribe(ShortStraddleConfig config, Scrip scrip) {
		if(log.isDebugEnabled()) log.debug("Unsubscribing for quotes for scrip {} and config {}", scrip, config);
		subscriptions.entrySet().stream()
			.filter(entry -> entry.getKey().equals(config))
			.map(Entry::getValue)
			.forEach(quoteSubscriptions -> quoteSubscriptions.removeIf(sub -> sub.getScrip().equals(scrip)));
	}
	
	private void closePosition(ShortStraddleConfig config) {
		if(log.isInfoEnabled()) log.info("Closing all open positions for config {}", config);
		positionService.findByTradingStrategyConfigIdAndStatusNot(config.getId(), PositionStatus.CLOSED).stream()
			.map(tradingStrategy::closePosition)
			.forEach(position -> save(position, config));
	}
	
	private void save(Position position, ShortStraddleConfig config) {
		positionService.save(position).getExitTrades().stream()
			.map(Trade::getScrip)
			.forEach(scrip -> unsubscribe(config, scrip));
	}
	
	@Override
	public void close() throws Exception {
		if(log.isInfoEnabled()) log.info("Closing trading strategy {}", getClass().getSimpleName());
		futures.values().stream()
			.flatMap(Collection::stream)
			.forEach(future -> future.cancel(false));
		futures.clear();
		subscriptions.values().stream()
			.flatMap(Collection::stream)
			.forEach(QuoteSubscription::unsubscribe);
		subscriptions.clear();
	}
}
