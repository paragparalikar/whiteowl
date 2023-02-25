package com.whiteowl.client.kite;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Holding;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.client.kite.model.Margin;
import com.whiteowl.client.kite.model.Order;
import com.whiteowl.client.kite.model.OrderId;
import com.whiteowl.client.kite.model.OrderType;
import com.whiteowl.client.kite.model.OrderValidity;
import com.whiteowl.client.kite.model.OrderVariety;
import com.whiteowl.client.kite.model.Position;
import com.whiteowl.client.kite.model.Profile;

import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KiteResilientClient implements KiteConnectApi {
	private static final String KITE = "kite";

	@NonNull private final KiteConnectApi delegate;
	
	private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.custom()
			.limitForPeriod(3)
			.limitRefreshPeriod(Duration.ofSeconds(1))
			.build());
	
	@Override
	public CandleSeries getData(long instrumentToken, String interval, ZonedDateTime from, ZonedDateTime to) {
		return Decorators.ofSupplier(() -> delegate.getData(instrumentToken, interval, from, to))
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public Profile getProfile() {
		return Decorators.ofSupplier(delegate::getProfile)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public Margin getMargin() {
		return Decorators.ofSupplier(delegate::getMargin)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public List<Holding> getHoldings() {
		return Decorators.ofSupplier(delegate::getHoldings)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public List<Position> getPositions() {
		return Decorators.ofSupplier(delegate::getPositions)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public List<Order> getOrders() {
		return Decorators.ofSupplier(delegate::getOrders)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public OrderId createOrder(Order order) {
		return Decorators.ofFunction(delegate::createOrder)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.apply(order);
	}

	@Override
	public OrderId update(Order order) {
		return Decorators.<Order, OrderId>ofFunction(delegate::update)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.apply(order);
	}

	@Override
	public OrderId update(OrderVariety variety, String orderId, OrderType orderType, int quantity,
			OrderValidity validity) {
		return Decorators.ofSupplier(() -> delegate.update(variety, orderId, orderType, quantity, validity))
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}

	@Override
	public OrderId cancel(Order order) {
		return Decorators.<Order, OrderId>ofFunction(delegate::cancel)
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.apply(order);
	}

	@Override
	public OrderId cancel(OrderVariety variety, String orderId) {
		return Decorators.ofSupplier(() -> delegate.cancel(variety, orderId))
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}
	
	@Override
	public Collection<KiteQuote> getQuotes(Collection<Instrument> instruments, KiteQuoteMode mode) {
		return Decorators.ofSupplier(() -> delegate.getQuotes(instruments, mode))
				.withRateLimiter(rateLimiterRegistry.rateLimiter(KITE))
				.get();
	}
	
	@Override
	public void subscribeOrderListener(Consumer<Order> orderListener) {
		delegate.subscribeOrderListener(orderListener);
	}
	
	@Override
	public void subscribeTickListener(Collection<Instrument> instruments, KiteQuoteMode mode,
			Consumer<KiteTick> tickListener) {
		delegate.subscribeTickListener(instruments, mode, tickListener);
	}
	
	@Override
	public void unsubscribeOrderListener(Consumer<Order> orderListener) {
		delegate.unsubscribeOrderListener(orderListener);
	}
	
	@Override
	public void unsubscribeTickListener(Consumer<KiteTick> tickListener) {
		delegate.unsubscribeTickListener(tickListener);
	}
	
	@Override
	public void close() throws Exception {
		delegate.close();
	}
	
}
