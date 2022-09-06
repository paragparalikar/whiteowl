package com.whiteowl.core.derivative.option;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;

@Service
public class OptionChainService {

	// TODO : Use hazelcast cache when jet is used
	private final Map<Scrip, OptionChain> cache = new HashMap<>();
	
	public Optional<OptionChain> findByScrip(Scrip scrip) {
		return Optional.ofNullable(cache.get(scrip));
	}
	
	public void save(OptionChain optionChain) {
		cache.put(optionChain.getUnderlying(), optionChain);
	}
	
}
