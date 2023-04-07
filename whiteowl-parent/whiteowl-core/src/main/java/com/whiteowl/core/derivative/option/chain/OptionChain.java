package com.whiteowl.core.derivative.option.chain;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChain {

	private Scrip underlying;
	private double spotPrice;
	private LocalDate expiry;
	private Set<Option> options;
	private ZonedDateTime timestamp;
	
}
