package com.whiteowl.core.derivative.option;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

@Service
public class OptionChainService {
	
	private final Map<Scrip, OptionChain> cache = new HashMap<>();
	
	public OptionChain save(@NonNull OptionChain optionChain) {
		cache.put(optionChain.getUnderlying(), optionChain);
		return optionChain;
	}
	
	public Optional<OptionChain> findByScrip(@NonNull Scrip scrip) {
		return Optional.ofNullable(cache.get(scrip));
	}
	
}
