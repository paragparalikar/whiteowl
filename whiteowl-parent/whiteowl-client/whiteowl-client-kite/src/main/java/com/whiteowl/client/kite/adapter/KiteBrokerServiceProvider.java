package com.whiteowl.client.kite.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.client.kite.model.Holding;
import com.whiteowl.client.kite.model.Order;
import com.whiteowl.client.kite.model.OrderId;
import com.whiteowl.client.kite.model.Position;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Credentials;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeProduct;

import lombok.RequiredArgsConstructor;

@Primary
@Component
@RequiredArgsConstructor
public class KiteBrokerServiceProvider implements BrokerServiceProvider {
	
	private final KiteMapper kiteMapper;
	private final KiteClientProvider kiteClientProvider;
	
	@Override
	public Broker getBrokerType() {
		return Broker.ZERODHA;
	}

	private KiteConnectApi getKiteConnectApi(Portfolio portfolio) {
		final Credentials credentials = portfolio.getCredentials();
		final KiteCredentials kiteCredentials = kiteMapper.toKiteCredentials(credentials);
		return kiteClientProvider.getClient(kiteCredentials);
	}
	
	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		final List<Order> orders = Optional.ofNullable(api.getOrders()).orElse(Collections.emptyList());
		return orders.stream()
				.map(order -> kiteMapper.toTrade(order))
				.collect(Collectors.toList());
	}
	
	@Override
	public void create(Trade trade, Portfolio portfolio) {
		final Order order = kiteMapper.toOrder(trade);
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		final OrderId orderId = api.createOrder(order);
		trade.setBrokerTradeId(orderId.getOrderId());
	}
	
	@Override
	public void update(Trade trade, Portfolio portfolio) {
		final Order order = kiteMapper.toOrder(trade);
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		api.update(order);
	}
	
	@Override
	public void cancel(Trade trade, Portfolio portfolio) {
		final Order order = kiteMapper.toOrder(trade);
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		api.cancel(order);
	}
	
	@Override
	public int getAvailableQuantity(@NonNull Scrip scrip, @NonNull Exchange exchange, 
			@NonNull TradeProduct product, @NonNull Portfolio portfolio) {
		final int holdingQuantity = getHoldingQuantity(scrip, exchange, product, portfolio);
		final int positionQuantity = getPositionQuantity(scrip, exchange, product, portfolio);
		return holdingQuantity + positionQuantity;
	}
	
	private int getHoldingQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio) {
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		return api.getHoldings().stream()
				.filter(holding -> scrip.getCode().equalsIgnoreCase(holding.getTradingsymbol()))
				.filter(holding -> exchange.equals(kiteMapper.toExchange(holding.getExchange())))
				.filter(holding -> product.equals(kiteMapper.toTradeProduct(holding.getProduct())))
				.collect(Collectors.summingInt(Holding::getQuantity));
	}
	
	private int getPositionQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio) {
		final KiteConnectApi api = getKiteConnectApi(portfolio);
		return api.getPositions().stream()
				.filter(position -> scrip.getCode().equalsIgnoreCase(position.getTradingsymbol()))
				.filter(position -> exchange.equals(kiteMapper.toExchange(position.getExchange())))
				.filter(position -> product.equals(kiteMapper.toTradeProduct(position.getProduct())))
				.collect(Collectors.summingInt(Position::getQuantity));
	}

}
