package com.whiteowl.nse.chain;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NseOptionChainProvider implements OptionChainProvider {
	
	private volatile String cookie;
	private final ScripService scripService;
	private final ObjectMapper objectMapper;
	private final Map<String, String> underlyingSymbolMapping = new HashMap<>();
	
	public NseOptionChainProvider(ObjectMapper objectMapper, ScripService scripService) {
		this.objectMapper = objectMapper;
		this.scripService = scripService;
		underlyingSymbolMapping.put("NIFTY 50", "NIFTY");
		underlyingSymbolMapping.put("NIFTY BANK", "BANKNIFTY");
		underlyingSymbolMapping.put("NIFTY FIN SERVICE", "FINNIFTY");
		underlyingSymbolMapping.put("NIFTY MID SELECT", "MIDCPNIFTY");
	}
	
	private String resolveUrl(Scrip underlying) {
		return Optional.ofNullable(underlyingSymbolMapping.get(underlying.getCode()))
				.map(code -> "https://www.nseindia.com/api/option-chain-indices?symbol=" + code)
				.orElseGet(() -> "https://www.nseindia.com/api/option-chain-equities?symbol=" + underlying.getCode());
	}
	
	@Override
	@SneakyThrows
	@Retryable(recover = "recover")
	public Optional<OptionChain> get(Scrip underlying) {
		final URL url = new URL(resolveUrl(underlying));
		final String cookie = Optional.ofNullable(this.cookie).orElseGet(() -> fetchCookie(url));
		final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("cookie", cookie);
		final InputStream inputStream = new GZIPInputStream(connection.getInputStream());
		final NseOptionChainResponse response = objectMapper.readValue(inputStream, NseOptionChainResponse.class);
		return Optional.of(map(underlying, response));
	}
	
	@SneakyThrows
	private synchronized String fetchCookie(URL url) {
		if(null == cookie) {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
			connection.setRequestProperty("accept-encoding", "gzip");
			connection.setConnectTimeout(1000);
			connection.setReadTimeout(3000);
			connection.getResponseCode();
			cookie = connection.getHeaderField("set-cookie");
			connection.disconnect();
		}
		return cookie;
	}
	
	@Recover
	public Optional<OptionChain> recover(Throwable throwable, Scrip underlying) {
		log.error("Failed to fetch option chain for " + underlying.getName(), throwable);
		return Optional.empty();
	}
	
	private OptionChain map(Scrip underlying, NseOptionChainResponse response) {
		final OptionChain chain = OptionChain.builder()
				.underlying(underlying)
				.build();
		chain.getExpiryDates().addAll(response.getRecords().getExpiryDates());
		for(NseOptionChainItem item : response.getRecords().getData()) {
			map(item.getPutOptionInfo()).ifPresent(chain.getItems()::add);
			map(item.getCallOptionInfo()).ifPresent(chain.getItems()::add);
		}
		return chain;
	}
	
	private Optional<OptionChainItem> map(NseOptionInfo info) {
		if(null == info) return Optional.empty();
		return Optional.ofNullable(info)
			.map(NseOptionInfo::toScripCode)
			.map(scripService::findByCode)
			.map(info::toOptionChainItem);
	}
}
