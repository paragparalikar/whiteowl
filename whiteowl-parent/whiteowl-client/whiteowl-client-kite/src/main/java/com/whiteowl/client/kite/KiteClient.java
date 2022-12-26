package com.whiteowl.client.kite;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.brotli.dec.BrotliInputStream;
import org.javalite.http.Get;
import org.javalite.http.Request;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Holding;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.Margin;
import com.whiteowl.client.kite.model.Order;
import com.whiteowl.client.kite.model.OrderId;
import com.whiteowl.client.kite.model.OrderType;
import com.whiteowl.client.kite.model.OrderValidity;
import com.whiteowl.client.kite.model.OrderVariety;
import com.whiteowl.client.kite.model.Position;
import com.whiteowl.client.kite.model.Profile;
import com.whiteowl.client.kite.model.Response;
import com.whiteowl.client.kite.request.CancelOrderRequest;
import com.whiteowl.client.kite.request.CreateOrderRequest;
import com.whiteowl.client.kite.request.HistoricalDataRequest;
import com.whiteowl.client.kite.request.HoldingsRequest;
import com.whiteowl.client.kite.request.MarginRequest;
import com.whiteowl.client.kite.request.OrdersRequest;
import com.whiteowl.client.kite.request.PositionsRequest;
import com.whiteowl.client.kite.request.ProfileRequest;
import com.whiteowl.client.kite.request.QuoteRequest;
import com.whiteowl.client.kite.request.UpdateOrderRequest;
import com.whiteowl.core.util.Constant;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KiteClient implements KiteConnectApi {
	
	@SneakyThrows
	public static List<Instrument> getInstruments(final KiteExchange exchange){
		final InputStream content = new Get(KiteConstant.URL_INSTRUMENTS + exchange.name(), 
				KiteConstant.TIMEOUT, KiteConstant.TIMEOUT).getInputStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(content));
		return reader.lines().map(Instrument::new).collect(Collectors.toList());
	}

	private final KiteSession session;
	
	@SneakyThrows
	private <T> T execute(Request<?> request, TypeReference<Response<T>> ref){
		final InputStream inputStream = resolveInputStream(request);
		final Response<T> response = KiteConstant.JSON.readValue(inputStream, ref);
		if(!"success".equalsIgnoreCase(response.getStatus())) {
			final String message = String.join(" - ", 
					String.valueOf(request.responseCode()),
					request.responseMessage(),
					response.getStatus(), 
					response.getErrorType(), 
					response.getMessage());
			final Field field = ReflectionUtils.findField(Request.class, "url");
			ReflectionUtils.makeAccessible(field);
			log.error(String.valueOf(ReflectionUtils.getField(field, request)));
			log.error(message);
			throw new RuntimeException(message);
		} else {
			return response.getData();
		}
	}
	
	@SneakyThrows
	private InputStream resolveInputStream(Request<?> request) {
		final String contentEncoding = getHeaderValue("content-encoding", request);
		if(StringUtils.hasText(contentEncoding)) {
			if("gzip".equalsIgnoreCase(contentEncoding)) {
				return new GZIPInputStream(request.getInputStream());
			} else if("br".equalsIgnoreCase(contentEncoding)) {
				return new BrotliInputStream(request.getInputStream());
			}
		} 
		return request.getInputStream();
	}
	
	private String getHeaderValue(String header, Request<?> request) {
		return request.headers().entrySet().stream()
				.filter(Objects::nonNull)
				.filter(entry -> header.equalsIgnoreCase(entry.getKey()))
				.map(Entry::getValue)
				.flatMap(Collection::stream)
				.findFirst()
				.orElse(null);
	}
	
	@Override
	@SneakyThrows
	public CandleSeries getData(long instrumentToken, String interval, ZonedDateTime from, ZonedDateTime to){
		to = truncate(to, interval, true);
		from = truncate(from, interval, false);
		final HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(
				instrumentToken, interval, from, to, session);
		session.authorize(historicalDataRequest);
		if(log.isDebugEnabled()) log.debug("Downloading data from kite for instrument {} {} from {} - to {}", instrumentToken, interval, from, to);
		final CandleSeries candleSeries = execute(historicalDataRequest, new TypeReference<Response<CandleSeries>>(){});
		return candleSeries;
	}
	
	private ZonedDateTime truncate(ZonedDateTime date, String interval, boolean toPrevious) {
		switch(interval) {
		case "day": return date.truncatedTo(ChronoUnit.DAYS);
		case "60minute": return truncateToPrevious(date, Duration.ofHours(1), toPrevious);
		case "30minute": return truncateToPrevious(date, Duration.ofMinutes(30), toPrevious);
		case "15minute": return truncateToPrevious(date, Duration.ofMinutes(15), toPrevious);
		case "10minute": return truncateToPrevious(date, Duration.ofMinutes(10), toPrevious);
		case "5minute": return truncateToPrevious(date, Duration.ofMinutes(5), toPrevious);
		}
		return date;
	}
	
	private ZonedDateTime truncateToPrevious(ZonedDateTime date, Duration duration, boolean toPrevious) {
		final ZonedDateTime startOfMarket = date
				.truncatedTo(ChronoUnit.DAYS)
				.plusHours(Constant.NSE_START_HOUR)
				.plusMinutes(Constant.NSE_START_MINUTE);
		final ZonedDateTime result = startOfMarket.plus(duration.multipliedBy(
	            Duration.between(startOfMarket, date).dividedBy(duration)));
		return toPrevious ? result.minus(duration) : result;
	}
	
	@Override
	@SneakyThrows
	public Collection<KiteQuote> getQuotes(Collection<Instrument> instruments, KiteQuoteMode mode) {
		return getQuotesMap(instruments, mode).values();
	}
	
	private Map<String, KiteQuote> getQuotesMap(Collection<Instrument> instruments, KiteQuoteMode mode){
		if(instruments.isEmpty()) return Collections.emptyMap();
		final QuoteRequest quoteRequest = new QuoteRequest(instruments, mode, session);
		session.authorize(quoteRequest);
		return execute(quoteRequest, new TypeReference<Response<Map<String, KiteQuote>>>(){});
	}
	
	@Override
	@SneakyThrows
	public Profile getProfile() {
		final ProfileRequest profileRequest = new ProfileRequest(session);
		session.authorize(profileRequest);
		return execute(profileRequest, new TypeReference<Response<Profile>>(){});
	}
	
	@Override
	@SneakyThrows
	public Margin getMargin() {
		final MarginRequest marginRequest = new MarginRequest(session);
		session.authorize(marginRequest);
		return execute(marginRequest, new TypeReference<Response<Margin>>(){});
	}
	
	
	@Override
	@SneakyThrows
	public List<Holding> getHoldings(){
		final HoldingsRequest holdingsRequest = new HoldingsRequest(session);
		session.authorize(holdingsRequest);
		return execute(holdingsRequest, new TypeReference<Response<List<Holding>>>(){});
	}
	
	@Override
	@SneakyThrows
	public List<Position> getPositions(){
		final PositionsRequest positionsRequest = new PositionsRequest(session);
		session.authorize(positionsRequest);
		return execute(positionsRequest, new TypeReference<Response<List<Position>>>(){});
	}
	
	@Override
	@SneakyThrows
	public List<Order> getOrders(){
		final OrdersRequest ordersRequest = new OrdersRequest(session);
		session.authorize(ordersRequest);
		return execute(ordersRequest, new TypeReference<Response<List<Order>>>(){});
	}
	
	@Override
	@SneakyThrows
	public OrderId createOrder(@NonNull final Order order) {
		final CreateOrderRequest createOrderRequest = new CreateOrderRequest(order, session);
		session.authorize(createOrderRequest);
		return execute(createOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
	
	@Override
	@SneakyThrows
	public OrderId update(@NonNull final Order order) {
		return update(order.getVariety(), order.getOrderId(), order.getOrderType(), 
				order.getQuantity(), order.getValidity());
	}
	
	@Override
	@SneakyThrows
	public OrderId update(@NonNull final OrderVariety variety, @NonNull final String orderId, 
			@NonNull final OrderType orderType, int quantity, @NonNull final OrderValidity validity) {
		final UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest(variety, orderId, orderType, quantity, validity, session);
		session.authorize(updateOrderRequest);
		return execute(updateOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
	
	@Override 
	@SneakyThrows
	public OrderId cancel(@NonNull final Order order) {
		return cancel(order.getVariety(), order.getOrderId());
	}
	
	@Override
	@SneakyThrows
	public OrderId cancel(@NonNull final OrderVariety variety, @NonNull final String orderId) {
		final CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(variety, orderId, session);
		session.authorize(cancelOrderRequest);
		return execute(cancelOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
	
}
