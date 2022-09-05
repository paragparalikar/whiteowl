package com.whiteowl.client.kite;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.Post;
import org.javalite.http.Request;
import org.springframework.util.ReflectionUtils;

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
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(KiteConstant.FORMAT_TIMESTAMP);
	
	@SneakyThrows
	private <T> T execute(Request<?> request, TypeReference<Response<T>> ref){
		final Response<T> response = KiteConstant.JSON.readValue(request.text(), ref);
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
	
	@Override
	@SneakyThrows
	public CandleSeries getData(long instrumentToken, String interval, ZonedDateTime from, ZonedDateTime to){
		final String url = String.join("/", KiteConstant.URL_BARS, String.valueOf(instrumentToken), interval) ;
		final Map<String, String> queryParams = new HashMap<>();
		queryParams.put("oi", "1");		
		queryParams.put("to", formatter.format(to));
		queryParams.put("from", formatter.format(from));
		queryParams.put("user_id", session.getCredentials().getUsername());
		final Get get = session.get(url + "?" + Http.map2URLEncoded(queryParams));
		return execute(get, new TypeReference<Response<CandleSeries>>(){});
	}
	
	@Override
	@SneakyThrows
	public Collection<KiteQuote> getQuotes(Collection<Instrument> instruments, KiteQuoteMode mode) {
		return getQuotesMap(instruments, mode).values();
	}
	
	private Map<String, KiteQuote> getQuotesMap(Collection<Instrument> instruments, KiteQuoteMode mode){
		if(instruments.isEmpty()) return Collections.emptyMap();
		final StringBuilder urlBuilder = new StringBuilder();
		switch(mode) {
		case FULL: urlBuilder.append(KiteConstant.URL_QUOTE); break;
		case LTP: urlBuilder.append(KiteConstant.URL_QUOTE_LTP); break;
		case OHLC: urlBuilder.append(KiteConstant.URL_QUOTE_OHLC); break;
		}
		final String queryString = instruments.stream()
			.map(instrument -> "i=" + instrument.getExchange().name() + ":" + instrument.getTradingsymbol())
			.collect(Collectors.joining("&"));
		urlBuilder.append("?" + queryString);
		return execute(session.get(urlBuilder.toString()), new TypeReference<Response<Map<String, KiteQuote>>>(){});
	}
	
	@Override
	@SneakyThrows
	public Profile getProfile() {
		return execute(session.get(KiteConstant.URL_PROFILE), new TypeReference<Response<Profile>>(){});
	}
	
	@Override
	@SneakyThrows
	public Margin getMargin() {
		return execute(session.delete(KiteConstant.URL_MARGIN), new TypeReference<Response<Margin>>(){});
	}
	
	@Override
	@SneakyThrows
	public List<Holding> getHoldings(){
		return execute(session.get(KiteConstant.URL_HOLDINGS), new TypeReference<Response<List<Holding>>>(){});
	}
	
	@Override
	@SneakyThrows
	public List<Position> getPositions(){
		return execute(session.get(KiteConstant.URL_POSITIONS), new TypeReference<Response<List<Position>>>(){});
	}
	
	@Override
	@SneakyThrows
	public List<Order> getOrders(){
		return execute(session.get(KiteConstant.URL_ORDERS), new TypeReference<Response<List<Order>>>(){});
	}
	
	@Override
	@SneakyThrows
	public OrderId createOrder(@NonNull final Order order) {
		final Post post = session.post(KiteConstant.URL_ORDERS + "/" + order.getVariety().name())
				.param("tradingsymbol", order.getTradingsymbol())
				.param("exchange", order.getExchange().name())
				.param("transaction_type", order.getTransactionType().name())
				.param("order_type", order.getOrderType().name())
				.param("quantity", String.valueOf(order.getQuantity()))
				.param("product", order.getProduct().name())
				.param("validity", order.getValidity().name());
		return execute(post, new TypeReference<Response<OrderId>>(){});
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
		final Post post = session.post(KiteConstant.URL_ORDERS + "/" + variety.name() + "/" + orderId)
				.param("order_type", orderType.name())
				.param("quantity", String.valueOf(quantity))
				.param("validity", validity.name());
		return execute(post, new TypeReference<Response<OrderId>>(){});
	}
	
	@Override 
	@SneakyThrows
	public OrderId cancel(@NonNull final Order order) {
		return cancel(order.getVariety(), order.getOrderId());
	}
	
	@Override
	@SneakyThrows
	public OrderId cancel(@NonNull final OrderVariety variety, @NonNull final String orderId) {
		final Delete delete = session.delete(KiteConstant.URL_ORDERS + "/" + variety.name() + "/" + orderId);
		return execute(delete, new TypeReference<Response<OrderId>>(){});
	}
	
}
