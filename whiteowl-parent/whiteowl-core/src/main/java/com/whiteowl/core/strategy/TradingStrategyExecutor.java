package com.whiteowl.core.strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.TradingStrategyConfigService;
import com.whiteowl.core.strategy.size.PositionSizingStrategy;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.util.Tuple2;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@Component
@RequiredArgsConstructor
public class TradingStrategyExecutor implements AutoCloseable {

	private final QuoteService quoteService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	private final PortfolioService portfolioService;
	private final TradingStrategyFactory tradingStrategyFactory;
	private final PositionSizingStrategy positionSizingStrategy;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	private final Collection<ScheduledFuture<?>> futures = new LinkedList<>();
	private final Map<Tuple2<Scrip, TradingStrategyConfig>, Consumer<Quote>> subscriptions = new ConcurrentHashMap<>();
	
	public void onApplicationReady() {
		for(TradingStrategyConfig config : tradingStrategyConfigService.findAll()){
			Stream.of(config.getEntryCronExpression(), config.getExitCronExpression())
				.filter(Objects::nonNull)
				.map(CronTrigger::new)
				.map(trigger -> taskScheduler.schedule(() -> execute(config), trigger))
				.forEach(futures::add);
		}
	}
	
	public void onScripBarDownloaded(
			@NonNull final Scrip scrip,
			@NonNull final Timeframe timeframe) {
		tradingStrategyConfigService
				.findByScripCodeAndTimeframe(scrip.getCode(), timeframe)
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
				.ifPresent(position -> handle(position, tradingStrategy, config));
		} else if(null == quote) {
			positions.stream()
				.filter(tradingStrategy::manage)
				.forEach(position -> handle(position, tradingStrategy, config));
		} else {
			positions.stream()
				.filter(position -> tradingStrategy.manage(position, quote))
				.forEach(position -> handle(position, tradingStrategy, config));
		}
	}
	
	public void onPositionSynchronized(@NonNull final Position position) {
		final String configId = position.getTradingStrategyConfigId();
		final TradingStrategyConfig config = tradingStrategyConfigService.findById(configId).orElseThrow();
		final TradingStrategy tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
		if(tradingStrategy.manage(position)) handle(position, tradingStrategy, config);
	}
	
	private void handle(Position position, TradingStrategy tradingStrategy, TradingStrategyConfig config) {
		if(null == position.getPortfolio()) {
			multicast(position, tradingStrategy, config);
		} else {
			position = positionService.save(position);
			if(position.getStatus().isTerminal()) unsubscribe(position, config);
			else subscribe(position, config);
		}
	}
	
	private void multicast(Position position, TradingStrategy tradingStrategy, TradingStrategyConfig config) {
		for(Portfolio portfolio : portfolioService.findAll()) {
			final Position clonePosition = position.withPortfolio(portfolio);
			final double amount = positionSizingStrategy.size(clonePosition, portfolio, config);
			if(tradingStrategy.quantify(clonePosition, amount)) {
				handle(clonePosition, tradingStrategy, config);
			}
		}
	}
	
	private void subscribe(Position position, TradingStrategyConfig config) {
		for(Trade entryTrade : position.getEntryTrades()) {
			final Scrip scrip = entryTrade.getScrip();
			final Tuple2<Scrip, TradingStrategyConfig> key = Tuple2.of(scrip, config);
			if(!subscriptions.containsKey(key)) {
				final Consumer<Quote> quoteListener = quote -> execute(config, quote);
				quoteService.subscribe(Collections.singleton(scrip), QuoteMode.FULL, quoteListener);
				subscriptions.put(key, quoteListener);
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
		subscriptions.values().forEach(quoteService::unsubscribe);
		subscriptions.clear();
	}
	
}
