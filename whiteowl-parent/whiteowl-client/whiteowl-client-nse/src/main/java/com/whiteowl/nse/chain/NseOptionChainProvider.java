package com.whiteowl.nse.chain;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.scrip.Scrip;

import lombok.SneakyThrows;

@Component
public class NseOptionChainProvider implements OptionChainProvider {
	
	private final ObjectMapper objectMapper;
	private final Map<String, String> underlyingSymbolMapping = new HashMap<>();
	
	public NseOptionChainProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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
	public Optional<OptionChain> get(Scrip underlying) {
		final URL url = new URL(resolveUrl(underlying));
		final NseOptionChainResponse response = objectMapper.readValue(url, NseOptionChainResponse.class);
		return null;
	}

}
