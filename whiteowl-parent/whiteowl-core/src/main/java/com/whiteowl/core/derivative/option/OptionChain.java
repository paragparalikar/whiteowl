package com.whiteowl.core.derivative.option;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

	private final IndexInfo indexInfo;
	private final Scrip underlying;
	private final double underlyingValue;
	private final LocalDateTime timestamp;
	private final Set<OptionChainItem> items = new HashSet<>(); 
	private final List<Double> strikePrices = new ArrayList<>();
	private final List<LocalDate> expiryDates = new ArrayList<>();
	
	public Optional<OptionChainItem> findByExpiryAndScripType(int skip, OptionExpiryType expiryType, ScripType scripType){
		return expiryType.resolve(skip, expiryDates)
				.flatMap(date -> findByExpiryAndScripType(date, scripType));
	}
	
	public Optional<OptionChainItem> findByExpiryAndScripType(LocalDate expiryDate, ScripType scripType){
		return items.stream()
				.filter(item -> item.getOptionInfo(scripType).getScrip().getExpiry().equals(expiryDate))
				.findFirst();
	}
	
	public Optional<OptionChainItem> findByScrip(@NonNull Scrip scrip){
		return items.stream()
				.filter(item -> scrip.equals(item.getOptionInfo(scrip.getType()).getScrip()))
				.findFirst();
	}

	public Optional<OptionInfo> findByDeltaAndScripType(double delta, @NonNull ScripType scripType){
		return items.stream()
				.map(item -> item.getOptionInfo(scripType))
				.min(Comparator.comparingDouble(info -> Math.abs(delta - info.getDelta())));
	}
	
}
