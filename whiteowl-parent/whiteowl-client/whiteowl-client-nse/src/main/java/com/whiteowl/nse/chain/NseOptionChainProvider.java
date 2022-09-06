package com.whiteowl.nse.chain;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

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
	
	public static void main(String[] args) throws Exception {
		final URL url = new URL("https://www.nseindia.com/api/option-chain-indices?symbol=NIFTY");
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
		connection.setRequestProperty("accept-encoding", "gzip");
		connection.setConnectTimeout(1000);
		connection.setReadTimeout(3000);
		if(401 == connection.getResponseCode()) {
			final String cookie = connection.getHeaderField("set-cookie");
			connection.disconnect();
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestProperty("cookie", cookie);
			connection.setConnectTimeout(1000);
			connection.setReadTimeout(3000);
		}
		
		final ObjectMapper objectMapper = new ObjectMapper();
		final NseOptionChainResponse response = objectMapper.readValue(new GZIPInputStream(connection.getInputStream()), NseOptionChainResponse.class);
		
		System.out.println(response);
	}
	
	@Override
	@SneakyThrows
	public Optional<OptionChain> get(Scrip underlying) {
		try {
			final URL url = new URL(resolveUrl(underlying));
			final NseOptionChainResponse response = objectMapper.readValue(url, NseOptionChainResponse.class);
			return Optional.of(map(underlying, response));
		} catch(Exception e) {
			log.error("", e);
			return Optional.empty();
		}
	}
	
	private OptionChain map(Scrip underlying, NseOptionChainResponse response) {
		final OptionChain chain = OptionChain.builder()
				.underlying(underlying)
				.build();
		for(NseOptionChainItem item : response.getRecords().getData()) {
			final NseOptionInfo callInfo = item.getCallOptionInfo();
			if(null != callInfo) {
				final String scripCode = callInfo.toScripCode();
				final Scrip scrip = scripService.findByCode(scripCode);
				final OptionChainItem optionChainItem = callInfo.toOptionChainItem();
				chain.getItems().put(scrip, optionChainItem);
			}
			final NseOptionInfo putInfo = item.getPutOptionInfo();
			if(null != putInfo) {
				final String scripCode = putInfo.toScripCode();
				final Scrip scrip = scripService.findByCode(scripCode);
				final OptionChainItem optionChainItem = putInfo.toOptionChainItem();
				chain.getItems().put(scrip, optionChainItem);
			}
		}
		return chain;
	}
	
}
