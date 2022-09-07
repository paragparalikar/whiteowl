package com.whiteowl.core.derivative.option;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChain {

	private final Scrip underlying;
	private final LocalDateTime downloadTimestamp;
	private final Map<Scrip, OptionChainItem> items = new HashMap<>(); 

	public Optional<OptionChainItem> findByDelta(double delta){
		return items.values().stream()
				.min(Comparator.comparingDouble(item -> Math.abs(delta - item.getDelta())));
	}
	
}
