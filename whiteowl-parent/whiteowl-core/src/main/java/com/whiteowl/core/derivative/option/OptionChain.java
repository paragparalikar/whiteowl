package com.whiteowl.core.derivative.option;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
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
	
}
