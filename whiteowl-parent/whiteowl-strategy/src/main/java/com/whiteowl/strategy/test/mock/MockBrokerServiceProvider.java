package com.whiteowl.strategy.test.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockBrokerServiceProvider implements BrokerServiceProvider {

	private final BarService barService;
	private final AtomicLong idGenerator = new AtomicLong();
	private final Map<Long, Trade> trades = new ConcurrentHashMap<>();
	
	@Override
	public Broker getBrokerType() {
		return Broker.TEST;
	}
	
	@Override
	public double getAvailableMargin(Portfolio portfolio) {
		return portfolio.getAvailableMargin();
	}

	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		return new ArrayList<>(trades.values());
	}

	@Override
	public void create(Trade trade, Portfolio portfolio) {
		if(!TradeStatus.PENDING.equals(trade.getStatus())) throw new IllegalStateException();
		if(null == trade.getId()) trade.setId(idGenerator.incrementAndGet());
		trade.setStatus(TradeStatus.OPEN);
		trades.put(trade.getId(), trade);
	}
	
	@Override
	public void update(Trade trade, Portfolio portfolio) {
		if(!TradeStatus.PENDING.equals(trade.getStatus())) throw new IllegalStateException();
		trade.setStatus(TradeStatus.OPEN);
		trades.put(trade.getId(), trade);
	}

	@Override
	public void cancel(Trade trade, Portfolio portfolio) {
		if(!TradeStatus.PENDING.equals(trade.getStatus())) throw new IllegalStateException();
		trade.setStatus(TradeStatus.CANCELLED);
	}

	@Override
	public int getAvailableQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}
	
	public void execute(@NonNull final Timeframe timeframe) {
		for(Trade trade : trades.values()) {
			if(TradeStatus.OPEN.equals(trade.getStatus())) {
				barService.findLatestBar(trade.getScrip().getCode(), timeframe)
					.ifPresent(bar -> execute(trade, bar));
			}
		}
	}
	
	private void execute(Trade trade, Bar bar) {
		if(TradeLimitType.LIMIT.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				
			}
		} else if(TradeLimitType.MARKET.equals(trade.getLimitType())) {
			
		}
		trade.setStatus(TradeStatus.COMPLETE);
	}

}
