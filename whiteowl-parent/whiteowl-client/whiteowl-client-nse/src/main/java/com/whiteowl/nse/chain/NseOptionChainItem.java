package com.whiteowl.nse.chain;

import java.time.LocalDate;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.derivative.option.OptionInfo;
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
		final OptionInfo callOptionInfo = null == this.callOptionInfo ? null : 
			this.callOptionInfo.toOptionInfo(scripResolver.apply(this.callOptionInfo.toScripCode()));
		final OptionInfo putOptionInfo = null == this.putOptionInfo ? null :
			this.putOptionInfo.toOptionInfo(scripResolver.apply(this.putOptionInfo.toScripCode()));
		return OptionChainItem.builder()
				.strikePrice(strikePrice)
				.expiryDate(expiryDate)
				.putOptionInfo(putOptionInfo)
				.callOptionInfo(callOptionInfo)
				.build();
	}
	
}
