package com.whiteowl.client.kite;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
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
import com.whiteowl.client.kite.util.Strings;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KiteHttpClient {

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
		final String content = Strings.toString(inputStream);
		if(log.isTraceEnabled()) log.trace(content);
		final Response<T> response = KiteConstant.JSON.readValue(content, ref);
		if(!"success".equalsIgnoreCase(response.getStatus())) {
			final String message = String.join(" - ", 
					String.valueOf(request.responseCode()),
					request.responseMessage(),
					response.getStatus(), 
					response.getErrorType(), 
					response.getMessage());
			final Field field = Request.class.getField("url");
			field.setAccessible(true);
			log.error(String.valueOf(field.get(request)));
			log.error(message);
			throw new RuntimeException(message);
		} else {
			return response.getData();
		}
	}
	
	@SneakyThrows
	private InputStream resolveInputStream(Request<?> request) {
		final String contentEncoding = getHeaderValue("content-encoding", request);
		if(Strings.hasText(contentEncoding)) {
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
	
	@SneakyThrows
	public CandleSeries getData(long instrumentToken, String interval, ZonedDateTime from, ZonedDateTime to){
		final HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(
				instrumentToken, interval, from, to, session);
		session.authorize(historicalDataRequest);
		if(log.isDebugEnabled()) log.debug("Downloading data from kite for instrument {} {} from {} - to {}", instrumentToken, interval, from, to);
		final CandleSeries candleSeries = execute(historicalDataRequest, new TypeReference<Response<CandleSeries>>(){});
		return candleSeries;
	}
	
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
	
	@SneakyThrows
	public Profile getProfile() {
		final ProfileRequest profileRequest = new ProfileRequest(session);
		session.authorize(profileRequest);
		return execute(profileRequest, new TypeReference<Response<Profile>>(){});
	}
	
	@SneakyThrows
	public Margin getMargin() {
		final MarginRequest marginRequest = new MarginRequest(session);
		session.authorize(marginRequest);
		return execute(marginRequest, new TypeReference<Response<Margin>>(){});
	}
	
	
	@SneakyThrows
	public List<Holding> getHoldings(){
		final HoldingsRequest holdingsRequest = new HoldingsRequest(session);
		session.authorize(holdingsRequest);
		return execute(holdingsRequest, new TypeReference<Response<List<Holding>>>(){});
	}
	
	@SneakyThrows
	public List<Position> getPositions(){
		final PositionsRequest positionsRequest = new PositionsRequest(session);
		session.authorize(positionsRequest);
		return execute(positionsRequest, new TypeReference<Response<List<Position>>>(){});
	}
	
	@SneakyThrows
	public List<Order> getOrders(){
		final OrdersRequest ordersRequest = new OrdersRequest(session);
		session.authorize(ordersRequest);
		return execute(ordersRequest, new TypeReference<Response<List<Order>>>(){});
	}
	
	@SneakyThrows
	public OrderId createOrder(@NonNull final Order order) {
		final CreateOrderRequest createOrderRequest = new CreateOrderRequest(order, session);
		session.authorize(createOrderRequest);
		return execute(createOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
	
	@SneakyThrows
	public OrderId update(@NonNull final Order order) {
		return update(order.getVariety(), order.getOrderId(), order.getOrderType(), 
				order.getQuantity(), order.getValidity());
	}
	
	@SneakyThrows
	public OrderId update(@NonNull final OrderVariety variety, @NonNull final String orderId, 
			@NonNull final OrderType orderType, int quantity, @NonNull final OrderValidity validity) {
		final UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest(variety, orderId, orderType, quantity, validity, session);
		session.authorize(updateOrderRequest);
		return execute(updateOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
	
	@SneakyThrows
	public OrderId cancel(@NonNull final Order order) {
		return cancel(order.getVariety(), order.getOrderId());
	}
	
	@SneakyThrows
	public OrderId cancel(@NonNull final OrderVariety variety, @NonNull final String orderId) {
		final CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(variety, orderId, session);
		session.authorize(cancelOrderRequest);
		return execute(cancelOrderRequest, new TypeReference<Response<OrderId>>(){});
	}
}
