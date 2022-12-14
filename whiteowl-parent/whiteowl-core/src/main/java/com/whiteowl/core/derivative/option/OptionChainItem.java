package com.whiteowl.core.derivative.option;

import java.time.LocalDate;

import com.whiteowl.core.scrip.ScripType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChainItem {

	private double strikePrice;
	
	private LocalDate expiryDate;
	
	private OptionInfo putOptionInfo;
	
	private OptionInfo callOptionInfo;
	
	public OptionInfo getOptionInfo(ScripType scripType) {
		switch(scripType) {
		case CE: return callOptionInfo;
		case PE: return putOptionInfo;
		default: throw new IllegalArgumentException(
				String.format("ScripType %s is not supported for options", scripType.name()));
		}
	}
}
