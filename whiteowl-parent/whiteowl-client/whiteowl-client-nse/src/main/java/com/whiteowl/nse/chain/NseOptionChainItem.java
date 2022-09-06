package com.whiteowl.nse.chain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import lombok.Data;

@Data
public class NseOptionChainItem {

	private double strikePrice;
	
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy")
	private Date expiryDate;
	
	@JsonProperty("PE")
	private NsePutOptionInfo putOptionInfo;
	
	@JsonProperty("CE")
	private NseCallOptionInfo callOptionInfo;
	
}
