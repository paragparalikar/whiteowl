package com.whiteowl.analysis.backtester.mock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.whiteowl.analysis.backtester.mock.broker.MockTradeExecutor;
import com.whiteowl.analysis.backtester.mock.broker.TradeExecutor;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.util.Trades;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockBrokerServiceProvider implements BrokerServiceProvider {
	
	private final double initialMargin, slippagePercentage;
	private final TradeExecutor tradeExecutor = new MockTradeExecutor();
	private final Map<Portfolio, PortfolioBrokerServiceProvider> providers = new ConcurrentHashMap<>();

	@Override
	public Broker getBrokerType() {
		return Broker.TEST;
	}
	
	private PortfolioBrokerServiceProvider getProvider(Portfolio portfolio) {
		return providers.computeIfAbsent(portfolio, key -> 
			new PortfolioBrokerServiceProvider(tradeExecutor, initialMargin, slippagePercentage));
	}

	@Override
	public double getAvailableMargin(Portfolio portfolio) {
		return getProvider(portfolio).getAvailableMargin();
	}

	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		return getProvider(portfolio).getAllTrades();
	}

	@Override
	public void create(Trade trade, Portfolio portfolio) {
		trade.setStatus(TradeStatus.PENDING);
		trade.setBrokerTradeId(UUID.randomUUID().toString());
		getProvider(portfolio).getActiveTrades().add(trade);
	}
	
	public void execute(Quote quote) {
		providers.values().forEach(provider -> provider.execute(quote));
	}
	
	public void expire(LocalDateTime timestamp) {
		providers.values().forEach(provider -> provider.expire(timestamp));
	}
	
	@Override
	public void update(Trade trade, Portfolio portfolio) {
		// The trade object is directly updated by trading strategy
	}

	@Override
	public void cancel(Trade trade, Portfolio portfolio) {
		final PortfolioBrokerServiceProvider provider = getProvider(portfolio);
		if(provider.getActiveTrades().contains(trade)) {
			trade.setStatus(TradeStatus.CANCELLED);
			Trades.setTimestamps(trade, trade.getCreatedDate());
			provider.getActiveTrades().remove(trade);
			provider.getTerminatedTrades().add(trade);
		} else {
			throw new IllegalArgumentException(String.format("Trade is not active - %s", trade.toString()));
		}
	}

	@Override
	public int getAvailableQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio) {
		return getProvider(portfolio).getAvailableQuantities().getOrDefault(scrip, 0);
	}

	@Override
	public void subscribeTradeStatusListener(Consumer<Trade> tradeStatusListener, Portfolio portfolio) {
		getProvider(portfolio).getListeners().add(tradeStatusListener);
	}

	@Override
	public void unsubscribeTradeStatusListener(Consumer<Trade> tradeStatusListener, Portfolio portfolio) {
		getProvider(portfolio).getListeners().remove(tradeStatusListener);
	}
	
}

@Getter
class PortfolioBrokerServiceProvider {
	
	private final TradeExecutor tradeExecutor;
	private double availableMargin, slippagePercentage;
	private final Map<Scrip, Integer> availableQuantities = new ConcurrentHashMap<>();
	private final Set<Trade> activeTrades = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<Trade> terminatedTrades = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<Consumer<Trade>> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
	
	public PortfolioBrokerServiceProvider(TradeExecutor tradeExecutor, double initialMargin, double slippagePercentage) {
		this.tradeExecutor = tradeExecutor;
		this.availableMargin = initialMargin;
		this.slippagePercentage = slippagePercentage;
	}
	
	public List<Trade> getAllTrades(){
		final List<Trade> allTrades = new ArrayList<>(activeTrades.size() + terminatedTrades.size());
		allTrades.addAll(activeTrades);
		allTrades.addAll(terminatedTrades);
		return allTrades;
	}
	
	public void expire(LocalDateTime timestamp) {
		final Iterator<Trade> activeTradeIterator = activeTrades.iterator();
		while(activeTradeIterator.hasNext()) {
			final Trade activeTrade = activeTradeIterator.next();
			activeTrade.setStatus(TradeStatus.CANCELLED);
			Trades.setTimestamps(activeTrade, timestamp);
			activeTradeIterator.remove();
			terminatedTrades.add(activeTrade);
			listeners.forEach(listener -> listener.accept(activeTrade));
		}
	}
	
	public void execute(Quote quote) {
		final Iterator<Trade> activeTradeIterator = activeTrades.iterator();
		while(activeTradeIterator.hasNext()) {
			final Trade activeTrade = activeTradeIterator.next();
			if(null == activeTrade.getCreatedDate()) activeTrade.setCreatedDate(quote.getTimestamp());
			if(activeTrade.getScrip().getCode().equals(quote.getCode())) {
				if(tradeExecutor.execute(activeTrade, quote, slippagePercentage)) {
					availableMargin += activeTrade.getAmount();
					activeTradeIterator.remove();
					terminatedTrades.add(activeTrade);
					listeners.forEach(listener -> listener.accept(activeTrade));
				}
			}
		}
	}
	
}
