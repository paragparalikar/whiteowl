package com.whiteowl.strategy.donchian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.Valid;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@Validated
@RequiredArgsConstructor
public class DonchianBreakoutTradingStrategyExecutor
		implements TradingStrategyExecutor<DonchianBreakoutTradingStrategyConfig> {

	private final BarService barService;
	private final QuoteService quoteService;
	private final PositionService positionService;
	private final DonchianBreakoutTradingStrategy tradingStrategy = new DonchianBreakoutTradingStrategy();
	@Getter private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.DONCHIAN;
	private final Map<DonchianBreakoutTradingStrategyConfig, List<QuoteSubscription>> subscriptions = new ConcurrentHashMap<>();
	
	@Override
	public void onScripBarDataDownloaded(
			@Valid @NonNull final DonchianBreakoutTradingStrategyConfig config, 
			@NonNull final Scrip scrip,
			@NonNull final Timeframe timeframe) {
		final long count = config.getBarCount();
		final List<Bar> bars = barService.findLatestByCodeAndTimeframe(scrip.getCode(), timeframe, count);
		if(bars.size() < count) return;
		Optional.ofNullable(tradingStrategy.openPosition(scrip, bars, config))
			.map(positionService::save)
			.ifPresent(position -> subscribe(config, position));
	}

	private void subscribe(DonchianBreakoutTradingStrategyConfig config, Position position) {
		position.getEntryTrades().stream()
			.map(Trade::getScrip)
			.map(quoteService::subscribe)
			.map(subscription -> subscription.addListener(quote -> onQuote(config, quote)))
			.forEach(subscriptions.computeIfAbsent(config, key -> Collections.synchronizedList(new ArrayList<>()))::add);
	}
	
	private void unsubscribe(DonchianBreakoutTradingStrategyConfig config, Position position) {
		position.getExitTrades().stream()
			.filter(trade -> trade.getStatus().isTerminal())
			.map(Trade::getScrip)
			.flatMap(scrip -> subscriptions.getOrDefault(config, Collections.emptyList()).stream()
								.filter(subscription -> Objects.equals(scrip, subscription.getScrip())))
			.forEach(QuoteSubscription::unsubscribe);
	}
	
	private void onQuote(DonchianBreakoutTradingStrategyConfig config, Quote quote) {
		positionService.findByTradingStrategyConfigIdAndStatusNot(config.getId(), PositionStatus.CLOSED).stream()
			.filter(position -> tradingStrategy.closePosition(config, position, quote))
			.map(positionService::save);
	}
	
	@Override
	public void onPositionChanged(
			@Valid @NonNull DonchianBreakoutTradingStrategyConfig config,
			@NonNull Position position) {
		unsubscribe(config, position);
	}
	
	@Override
	public void close() throws Exception {
		subscriptions.values().stream()
			.flatMap(Collection::stream)
			.forEach(QuoteSubscription::unsubscribe);
		subscriptions.clear();
	}

}
