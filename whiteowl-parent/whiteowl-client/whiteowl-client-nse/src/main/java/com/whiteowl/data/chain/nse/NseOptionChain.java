package com.whiteowl.data.chain.nse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NseOptionChain {

	private final List<LocalDate> expiryDates = new ArrayList<>();
	
	private final List<Double> strikePrices = new ArrayList<>();
	
	private final List<NseOptionChainItem> data = new ArrayList<>();
	
	private LocalDateTime timestamp;
	
	private double underlyingValue;
	
	private NseIndexInfo index;

}
