package com.whiteowl.core.derivative.option;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class OptionChain {

	private final Scrip underlying;
	private final LocalDateTime downloadTimestamp;
	private final Set<OptionChainItem> items = new HashSet<>(); 
	
	public Optional<OptionChainItem> findByScrip(@NonNull Scrip scrip){
		return items.stream()
				.filter(item -> scrip.equals(item.getScrip()))
				.findFirst();
	}

	public Optional<OptionChainItem> findByDeltaAndScripType(double delta, @NonNull ScripType scripType){
		return items.stream()
				.filter(item -> scripType.equals(item.getScrip().getType()))
				.min(Comparator.comparingDouble(item -> Math.abs(delta - item.getDelta())));
	}
	
}
