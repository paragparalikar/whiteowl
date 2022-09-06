package com.whiteowl.core.derivative.option;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;

@Service
public class OptionChainService {
	
	// TODO : Use hazelcast cache when jet is used
	@Autowired private OptionChainProvider optionChainProvider;
	@Value("${whiteowl.option-chain.delay.seconds:15}") private int delaySeconds;
	private final Map<Scrip, OptionChain> cache = new HashMap<>();
	
	public Optional<OptionChain> findByScrip(Scrip scrip) {
		OptionChain chain = cache.computeIfAbsent(scrip, optionChainProvider::get);
		if(null == chain) cache.remove(scrip);
		else if(Duration.between(LocalDateTime.now(), chain.getDownloadTimestamp()).abs().toSeconds() >= delaySeconds) {
			chain = optionChainProvider.get(scrip);
			if(null != chain) cache.put(scrip, chain);
		}
		return Optional.ofNullable(chain);
	}
	
}
