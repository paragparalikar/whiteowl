package com.whiteowl.core.derivative.option;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

@Service
public class OptionChainService {
	
	@Autowired private OptionChainProvider optionChainProvider;
	@Value("${whiteowl.option-chain.ttl:1m}") private Duration ttl;
	private final Map<Scrip, OptionChain> cache = new HashMap<>();
	private final Map<Scrip, LocalTime> timestampCache = new HashMap<>();
	// TODO : Use Hazelcast
	
	public Optional<OptionChain> findByScrip(@NonNull Scrip scrip) {
		if(isAbsent(scrip) || isExpired(scrip)) load(scrip);
		return Optional.ofNullable(cache.get(scrip));
	}
	
	private void load(Scrip scrip) {
		optionChainProvider.get(scrip).ifPresent(chain -> cache.put(scrip, chain));
		timestampCache.put(scrip, LocalTime.now());
	}
	
	private boolean isAbsent(Scrip scrip) {
		return !cache.containsKey(scrip);
	}
	
	private boolean isExpired(Scrip scrip) {
		return LocalTime.now().minus(ttl).isAfter(
				timestampCache.getOrDefault(scrip, LocalTime.MIN));
	}
	
}
