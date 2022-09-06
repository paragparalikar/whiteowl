package com.whiteowl.nse.chain;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.scrip.ScripType;

import lombok.SneakyThrows;

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
	
	@Override
	@SneakyThrows
	public Optional<OptionChain> get(Scrip underlying) {
		final URL url = new URL(resolveUrl(underlying));
		final NseOptionChainResponse response = objectMapper.readValue(url, NseOptionChainResponse.class);
		return Optional.of(map(underlying, response));
	}
	
	private OptionChain map(Scrip underlying, NseOptionChainResponse response) {
		final OptionChain chain = OptionChain.builder()
				.underlying(underlying)
				.build();
		for(NseOptionChainItem item : response.getRecords().getData()) {
			final NseOptionInfo callInfo = item.getCallOptionInfo();
			if(null != callInfo) {
				final String scripCode = callInfo.toScripCode(ScripType.CE);
				final Scrip scrip = scripService.findByCode(scripCode);
				final OptionChainItem optionChainItem = callInfo.toOptionChainItem();
				chain.getItems().put(scrip, optionChainItem);
			}
			final NseOptionInfo putInfo = item.getPutOptionInfo();
			if(null != putInfo) {
				final String scripCode = putInfo.toScripCode(ScripType.PE);
				final Scrip scrip = scripService.findByCode(scripCode);
				final OptionChainItem optionChainItem = putInfo.toOptionChainItem();
				chain.getItems().put(scrip, optionChainItem);
			}
		}
		return chain;
	}
	
}
