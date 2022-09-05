package com.whiteowl.data.chain.nse;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class NseOptionChainItem {

	private double strikePrice;
	
	private LocalDate expiryDate;
	
	@JsonProperty("PE")
	private NseOptionInfo putOptionInfo;
	
	@JsonProperty("CE")
	private NseOptionInfo callOptionInfo;
	
}
