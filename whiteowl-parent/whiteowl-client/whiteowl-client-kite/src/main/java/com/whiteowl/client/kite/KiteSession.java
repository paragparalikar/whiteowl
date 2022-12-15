package com.whiteowl.client.kite;

import java.net.HttpCookie;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.Post;
import org.javalite.http.Put;
import org.javalite.http.Request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.whiteowl.client.kite.model.Response;
import com.whiteowl.client.kite.model.Twofa;
import com.whiteowl.client.kite.ticker.KiteTicker;
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
	
	@NonNull private final KiteCredentials credentials;
	private final Set<HttpCookie> cookies = new HashSet<>();
	
	private String cookies() {
		return cookies.stream().map(Object::toString).collect(Collectors.joining("; "));
	}
	
	private void cookies(Map<String, List<String>> headers) {
		final List<String> values = new LinkedList<>();
		Optional.ofNullable(headers.get("set-cookie")).ifPresent(cookies -> values.addAll(cookies));
		Optional.ofNullable(headers.get("Set-Cookie")).ifPresent(cookies -> values.addAll(cookies));
		values.stream().map(HttpCookie::parse).flatMap(Collection::stream).forEach(cookies::add);
	}
	
	@SneakyThrows
	private void login() {
		cookies(new Get(KiteConstant.URL_BASE, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT)
				.header(KiteConstant.USER_AGENT, KiteConstant.USER_AGENT_CHROME)
				.headers());
		final Post login = new Post(KiteConstant.URL_BASE + KiteConstant.URL_LOGIN, null, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT)
				.header(KiteConstant.USER_AGENT, KiteConstant.USER_AGENT_CHROME)
				.header("cookie", cookies())
				.param("user_id", credentials.getUsername())
				.param("password", credentials.getPassword());
		final Response<Twofa> response = KiteConstant.JSON.readValue(login.text(), new TypeReference<Response<Twofa>>(){});
		final Twofa twofaInfo = response.getData();
		if(twofaInfo.isLocked()) {
			throw new IllegalStateException(String.format("Kite account is locked for %s. Manual intervention is required.", 
					credentials.getUsername()));
		}
		if(twofaInfo.isCaptcha()) {
			throw new IllegalStateException(String.format("Kite api has requested CAPTCHA for %s. Manual intervention is required.", 
					credentials.getUsername()));
		}
		final Post twofa = new Post(KiteConstant.URL_BASE + KiteConstant.URL_TWOFA, null, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT)
				.header(KiteConstant.USER_AGENT, KiteConstant.USER_AGENT_CHROME)
				.header("cookie", cookies())
				.param("user_id", twofaInfo.getUserId())
				.param("request_id", twofaInfo.getRequestId())
				.param("twofa_type", twofaInfo.getTwofaType())
				.param("twofa_value", toTotp(credentials.getPin()));
		
		cookies(twofa.headers());
		cookies(get(KiteConstant.URL_DASHBOARD).headers());
	}
	
	private String toTotp(String pin) throws InterruptedException {
		final LocalDateTime localDateTime = LocalDateTime.now();
		final int secondOfMinute = localDateTime.getSecond();
		if(secondOfMinute >= 27 && secondOfMinute <= 29) Thread.sleep(30 - secondOfMinute);
		if(secondOfMinute >= 57 && secondOfMinute <= 59) Thread.sleep(60 - secondOfMinute);
		final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
		return String.valueOf(googleAuthenticator.getTotpPassword(pin));
	}
	
	public KiteTicker createTicker() {
		if(cookies.isEmpty() || !Strings.hasText(getCookieValue("enctoken"))) login();
		return new KiteTicker(credentials.getUsername(), getCookieValue("enctoken"));
	}
	
	public Get get(final String url) {
		return authorize(new Get(KiteConstant.URL_BASE + url, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT));
	}
	
	public Delete delete(final String url) {
		return authorize(new Delete(KiteConstant.URL_BASE + url, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT));
	}
	
	public Post post(final String url) {
		return authorize(new Post(KiteConstant.URL_BASE + url, null, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT));
	}
	
	public Put put(final String url) {
		return authorize(new Put(KiteConstant.URL_BASE + url, null, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT));
	}
	
	private String getCookieValue(String cookieName) {
		return cookies.stream()
				.filter(cookie -> cookieName.equalsIgnoreCase(cookie.getName()))
				.map(HttpCookie::getValue)
				.findFirst().orElse(null);
	}
	
	private <T extends Request<T>> T authorize(final T request){
		addHeaders(request);
		if(403 == request.responseCode()) {
			cookies.clear();
			addHeaders(request);
		}
		if(400 <= request.responseCode()) {
			log.error(request.responseCode() + " : " + request.responseMessage() + "\n" + request.text());
		}
		return request;
	}
	
	private  void addHeaders(final Request<?> request){
		final String enctoken = getCookieValue("enctoken");
		if(cookies.isEmpty() || !Strings.hasText(enctoken)) login();
		request
			.header("cookie", cookies())
			.header("authorization", "enctoken " + enctoken)
			.header(KiteConstant.USER_AGENT, KiteConstant.USER_AGENT_CHROME);
	}

}
