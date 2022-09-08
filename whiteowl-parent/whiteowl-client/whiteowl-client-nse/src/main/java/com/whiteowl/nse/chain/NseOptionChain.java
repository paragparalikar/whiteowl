package com.whiteowl.nse.chain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NseOptionChain {

	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy")
	private final List<LocalDate> expiryDates = new ArrayList<>();

	private final List<Double> strikePrices = new ArrayList<>();
	
	private final List<NseOptionChainItem> data = new ArrayList<>();
	
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy HH:mm:ss")
	private LocalDateTime timestamp;
	
	private double underlyingValue;
	
	private NseIndexInfo index;

}
