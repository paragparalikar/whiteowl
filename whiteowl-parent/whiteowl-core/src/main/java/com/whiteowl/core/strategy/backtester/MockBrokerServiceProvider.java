package com.whiteowl.core.strategy.backtester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockBrokerServiceProvider implements BrokerServiceProvider {
	
	private final double initialMargin, slippagePercentage;
	private final Map<Portfolio, PortfolioBrokerServiceProvider> providers = new ConcurrentHashMap<>();

	@Override
	public Broker getBrokerType() {
		return Broker.TEST;
	}
	
	private PortfolioBrokerServiceProvider getProvider(Portfolio portfolio) {
		return providers.computeIfAbsent(portfolio, key -> new PortfolioBrokerServiceProvider(initialMargin));
	}

	@Override
	public double getAvailableMargin(Portfolio portfolio) {
		return getProvider(portfolio).getAvailableMargin();
	}

	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		return new ArrayList<>(getProvider(portfolio).getTrades());
	}

	@Override
	public void create(Trade trade, Portfolio portfolio) {
		if(0 == trade.getPrice()) throw new IllegalArgumentException("Trade price must be provided for backtesting");
		trade.setBrokerTradeId(UUID.randomUUID().toString());
		getProvider(portfolio).getTrades().add(trade);
		final double buyPrice = trade.getPrice() * (100 + slippagePercentage) / 100;
		final double sellPrice = trade.getPrice() * (100 - slippagePercentage) / 100;
		trade.setAveragePrice(TradeType.BUY.equals(trade.getType()) ? buyPrice : sellPrice);
		getProvider(portfolio).getListeners().forEach(listener -> listener.accept(trade));
		trade.setStatus(TradeStatus.COMPLETE);
	}

	@Override
	public void update(Trade trade, Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cancel(Trade trade, Portfolio portfolio) {
		throw new UnsupportedOperationException();
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
	
	private double availableMargin;
	private final Map<Scrip, Integer> availableQuantities = new ConcurrentHashMap<>();
	private final Set<Trade> trades = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<Consumer<Trade>> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
	
	public PortfolioBrokerServiceProvider(double initialMargin) {
		this.availableMargin = initialMargin;
	}
	
}
