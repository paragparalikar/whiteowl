package com.whiteowl.core.analysis.backtester;

import static com.whiteowl.core.trade.TradeLimitType.LIMIT;
import static com.whiteowl.core.trade.TradeLimitType.MARKET;
import static com.whiteowl.core.trade.TradeLimitType.SL;
import static com.whiteowl.core.trade.TradeLimitType.SLM;
import static com.whiteowl.core.trade.TradeStatus.COMPLETE;
import static org.ta4j.core.Trade.TradeType.BUY;

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

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
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
		return providers.computeIfAbsent(portfolio, key -> 
			new PortfolioBrokerServiceProvider(initialMargin, slippagePercentage));
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
		if(0 == trade.getPrice()) throw new IllegalArgumentException("Trade price must be provided for backtesting");
		trade.setStatus(TradeStatus.PENDING);
		trade.setBrokerTradeId(UUID.randomUUID().toString());
		getProvider(portfolio).getActiveTrades().add(trade);
	}
	
	public void execute(Bar bar, Scrip scrip) {
		providers.values().forEach(provider -> provider.execute(bar, scrip));
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
	
	private double availableMargin, slippagePercentage;
	private final Map<Scrip, Integer> availableQuantities = new ConcurrentHashMap<>();
	private final Set<Trade> activeTrades = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<Trade> terminatedTrades = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<Consumer<Trade>> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
	
	public PortfolioBrokerServiceProvider(double initialMargin, double slippagePercentage) {
		this.availableMargin = initialMargin;
		this.slippagePercentage = slippagePercentage;
	}
	
	public List<Trade> getAllTrades(){
		final List<Trade> allTrades = new ArrayList<>(activeTrades.size() + terminatedTrades.size());
		allTrades.addAll(activeTrades);
		allTrades.addAll(terminatedTrades);
		return allTrades;
	}
	
	public void execute(Bar bar, Scrip scrip) {
		final Iterator<Trade> activeTradeIterator = activeTrades.iterator();
		while(activeTradeIterator.hasNext()) {
			final Trade activeTrade = activeTradeIterator.next();
			if(scrip.equals(activeTrade.getScrip())) {
				final TradeType type = activeTrade.getType();
				final TradeLimitType limitType = activeTrade.getLimitType();
				if((SL.equals(limitType) || SLM.equals(limitType)) 
						&& (activeTrade.getTriggerPrice() < bar.getLowPrice().doubleValue()
								|| activeTrade.getTriggerPrice() > bar.getHighPrice().doubleValue())) {
					continue;
				}
				
				if(LIMIT.equals(limitType) 
						&& ((BUY.equals(type) && activeTrade.getPrice() < bar.getLowPrice().doubleValue())
								|| (TradeType.SELL.equals(type) && activeTrade.getPrice() > bar.getHighPrice().doubleValue()))) {
					continue;
				}
				
				final double price = MARKET.equals(limitType) ?
						bar.getOpenPrice().doubleValue() : activeTrade.getPrice();
				final double buyPrice = price * (100 + slippagePercentage) / 100;
				final double sellPrice = price * (100 - slippagePercentage) / 100;
				activeTrade.setAveragePrice(BUY.equals(type) ? buyPrice : sellPrice);
				activeTrade.setStatus(COMPLETE);
				activeTrade.setFilledQuantity(activeTrade.getQuantity());
				setTimestamps(bar, activeTrade);
				availableMargin += activeTrade.getAmount();
				activeTradeIterator.remove();
				terminatedTrades.add(activeTrade);
				listeners.forEach(listener -> listener.accept(activeTrade));
			}
		}
	}
	
	private void setTimestamps(Bar bar, Trade trade) {
		final LocalDateTime timestamp = bar.getBeginTime().toLocalDateTime();
		trade.setCreatedDate(timestamp);
		trade.setExchangeTimestamp(timestamp);
		trade.setLastModifiedDate(timestamp);
		trade.setTimestamp(timestamp);
	}
	
}
