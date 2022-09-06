package com.whiteowl.nse.chain;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class NseOptionChainItem {

	private double strikePrice;
	
	private LocalDate expiryDate;
	
	@JsonProperty("PE")
	private NsePutOptionInfo putOptionInfo;
	
	@JsonProperty("CE")
	private NseCallOptionInfo callOptionInfo;
	
}
