package com.whiteowl.strategy.shortstrangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
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

@Component
@RequiredArgsConstructor
public class ShortStraddleTradingStrategyExecutor implements TradingStrategyExecutor<ShortStraddleConfig> {

	private final ScripService scripService;
	private final QuoteService quoteService;
	private final PositionService positionService;
	private final OptionChainService optionChainService;
	private final List<ScheduledFuture<?>> futures = new ArrayList<>(2);
	private final List<QuoteSubscription> subscriptions = new ArrayList<>(2);
	private final ShortStraddleTradingStrategy tradingStrategy = new ShortStraddleTradingStrategy();
	@Getter private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;
	
	@Override
	public void schedule(@NonNull final ShortStraddleConfig config, @NonNull final TaskScheduler taskScheduler) {
		futures.add(taskScheduler.schedule(() -> openPosition(config), new CronTrigger(config.getPositionOpenCron())));
		futures.add(taskScheduler.schedule(() -> closePosition(config), new CronTrigger(config.getPositionCloseCron())));
	}
	
	private void openPosition(ShortStraddleConfig config) {
		final Scrip scrip = scripService.findByCode(Index.NIFTY50.getCode());
		final OptionChain optionChain = optionChainService.findByScrip(scrip).orElseThrow();
		final Position position = tradingStrategy.openPosition(optionChain, config);
		position.getEntryTrades().stream()
			.map(Trade::getScrip)
			.map(tradeScrip -> quoteService.subscribe(tradeScrip, QuoteMode.LTP))
			.map(subscription -> subscription.addListener(quote -> onQuote(config, quote)))
			.forEach(subscriptions::add);
		// TODO send position to broker
	}
	
	private void onQuote(ShortStraddleConfig config, Quote quote) {
		final List<Position> positions = positionService.findByTradingStrategyConfigId(config.getId());
		
	}
	
	
	private void closePosition(ShortStraddleConfig config) {
		Optional.ofNullable(putQuoteSubscription).ifPresent(QuoteSubscription::unsubscribe);
		Optional.ofNullable(callQuoteSubscription).ifPresent(QuoteSubscription::unsubscribe);
	}
	
	@Override
	public void close() throws Exception {
		futures.forEach(future -> future.cancel(false));
		futures.clear();
		subscriptions.forEach(QuoteSubscription::unsubscribe);
		subscriptions.clear();
	}
}
