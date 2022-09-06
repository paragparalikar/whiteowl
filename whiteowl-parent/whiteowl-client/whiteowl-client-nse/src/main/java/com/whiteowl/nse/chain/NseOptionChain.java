package com.whiteowl.nse.chain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NseOptionChain {

	private final List<Double> strikePrices = new ArrayList<>();
	
	private final List<NseOptionChainItem> data = new ArrayList<>();
	
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy HH:mm:ss")
	private Date timestamp;
	
	private double underlyingValue;
	
	private NseIndexInfo index;

}
