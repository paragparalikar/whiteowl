package com.whiteowl.client.kite;

import static com.whiteowl.client.kite.KiteConstant.JSON;
import static com.whiteowl.core.util.Http.AUTHORIZATION;
import static com.whiteowl.core.util.Http.BAD_REQUEST;
import static com.whiteowl.core.util.Http.BR;
import static com.whiteowl.core.util.Http.CONTENT_ENCODING;
import static com.whiteowl.core.util.Http.COOKIE;
import static com.whiteowl.core.util.Http.FORBIDDEN;
import static com.whiteowl.core.util.Http.GZIP;
import static com.whiteowl.core.util.Http.SET_COOKIE;

import java.io.InputStream;
import java.net.HttpCookie;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.brotli.dec.BrotliInputStream;
import org.javalite.http.Request;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.whiteowl.client.kite.model.Response;
import com.whiteowl.client.kite.model.Twofa;
import com.whiteowl.client.kite.request.LoginRequest;
import com.whiteowl.client.kite.request.RootRequest;
import com.whiteowl.client.kite.request.TwofaRequest;
import com.whiteowl.core.util.Strings;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@RequiredArgsConstructor
public class KiteSession {
	private static final String ENCTOKEN = "enctoken";

	private final UUID uuid = UUID.randomUUID();
	@NonNull private final KiteCredentials credentials;
	private final Set<HttpCookie> cookies = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public void setCookies(Request<?> request) {
		request.headers().entrySet().stream()
			.filter(Objects::nonNull)
			.filter(entry -> SET_COOKIE.equalsIgnoreCase(entry.getKey()))
			.map(Entry::getValue)
			.flatMap(Collection::stream)
			.map(HttpCookie::parse)
			.flatMap(Collection::stream)
			.forEach(cookies::add);
	}
	
	public String getCookies() {
		return cookies.stream()
				.map(Object::toString)
				.collect(Collectors.joining(";"));
	}
	
	@SneakyThrows
	private void login() {
		setCookies(new RootRequest());
		final LoginRequest loginRequest = new LoginRequest(this);
		setCookies(loginRequest);
		final InputStream inputStream = resolveInputStream(loginRequest);
		final Response<Twofa> response = JSON.readValue(inputStream, new TypeReference<Response<Twofa>>(){});
		final Twofa twofaInfo = response.getData();
		if(twofaInfo.isLocked()) {
			throw new IllegalStateException(String.format("Kite account is locked for %s. Manual intervention is required.", 
					credentials.getUsername()));
		}
		if(twofaInfo.isCaptcha()) {
			throw new IllegalStateException(String.format("Kite api has requested CAPTCHA for %s. Manual intervention is required.", 
					credentials.getUsername()));
		}
		final TwofaRequest twofaRequest = new TwofaRequest(twofaInfo, this);
		setCookies(twofaRequest);
	}
	
	@SneakyThrows
	private InputStream resolveInputStream(Request<?> request) {
		final String contentEncoding = getHeaderValue(CONTENT_ENCODING, request);
		if(StringUtils.hasText(contentEncoding)) {
			if(GZIP.equalsIgnoreCase(contentEncoding)) {
				return new GZIPInputStream(request.getInputStream());
			} else if(BR.equalsIgnoreCase(contentEncoding)) {
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
	private String getCookieValue(String cookieName) {
		return cookies.stream()
				.filter(cookie -> cookieName.equalsIgnoreCase(cookie.getName()))
				.map(HttpCookie::getValue)
				.findFirst().orElse(null);
	}
	
	public synchronized <T extends Request<T>> T authorize(final T request){
		addHeaders(request);
		if(FORBIDDEN == request.responseCode()) {
			login();
			addHeaders(request);
		}
		if(BAD_REQUEST <= request.responseCode()) {
			log.error(request.responseCode() + " : " + request.responseMessage() + "\n" + request.text());
		}
		return request;
	}
	
	private void addHeaders(final Request<?> request){
		final String enctoken = getEncToken();
		request.header(COOKIE, getCookies());
		request.header(AUTHORIZATION, ENCTOKEN + " " + enctoken);
	}
	
	String getEncToken() {
		String enctoken = getCookieValue(ENCTOKEN);
		if(!Strings.hasText(enctoken)) {
			login();
			enctoken = getCookieValue(ENCTOKEN);
		}
		return enctoken;
	}
}
