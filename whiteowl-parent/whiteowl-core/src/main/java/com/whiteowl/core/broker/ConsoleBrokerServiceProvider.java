package com.whiteowl.core.broker;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ConsoleBrokerServiceProvider implements BrokerServiceProvider {
	
	private final Set<Trade> trades = new HashSet<>();
	
	@Override
	public Broker getBrokerType() {
		return Broker.CONSOLE;
	}

	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		return new ArrayList<>(trades);
	}

	@Override
	public void create(Trade trade, Portfolio portfolio) {
		trade.setStatus(TradeStatus.COMPLETE);
		trade.setTimestamp(ZonedDateTime.now());
		trade.setFilledQuantity(trade.getQuantity());
		trade.setExchangeTimestamp(ZonedDateTime.now());
		trade.setBrokerTradeId(UUID.randomUUID().toString());
		trade.setExchangeTradeId(UUID.randomUUID().toString());
		trades.add(trade);
		log.info("Created trade {}", trade);
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
		return 0;
	}

}
