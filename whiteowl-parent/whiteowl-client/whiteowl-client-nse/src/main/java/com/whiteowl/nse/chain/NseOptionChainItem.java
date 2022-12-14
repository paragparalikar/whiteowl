package com.whiteowl.nse.chain;

import java.time.LocalDate;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.scrip.Scrip;

import lombok.Data;

@Data
public class NseOptionChainItem {

	private double strikePrice;
	
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy")
	private LocalDate expiryDate;
	
	@JsonProperty("PE")
	private NsePutOptionInfo putOptionInfo;
	
	@JsonProperty("CE")
	private NseCallOptionInfo callOptionInfo;
	
	public OptionChainItem toOptionChainItem(Scrip scrip, Function<String, Scrip> scripResolver) {
		return OptionChainItem.builder()
				.strikePrice(strikePrice)
				.expiryDate(expiryDate)
				.putOptionInfo(putOptionInfo.toOptionInfo(scripResolver.apply(putOptionInfo.toScripCode())))
				.callOptionInfo(callOptionInfo.toOptionInfo(scripResolver.apply(callOptionInfo.toScripCode())))
				.build();
	}
	
}
