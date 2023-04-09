package com.whiteowl.core.derivative.option.chain;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.scrip.StrikeType;

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
	
	public Set<Option> getOptions(ScripType scripType){
		return options.stream()
				.filter(option -> scripType.equals(option.getType()))
				.collect(Collectors.toSet());
	}
	
	public Set<Option> getOptions(ScripType scripType, StrikeType strikeType){
		return options.stream()
				.filter(option -> scripType.equals(option.getType()))
				.filter(option -> strikeType.equals(option.getStrikeType(spotPrice)))
				.collect(Collectors.toSet());
	}
	
}
