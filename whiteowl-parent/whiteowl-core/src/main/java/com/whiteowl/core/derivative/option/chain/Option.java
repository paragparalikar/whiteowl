package com.whiteowl.core.derivative.option.chain;

import com.whiteowl.core.scrip.ScripType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(of = "code")
public class Option {

	private String code;
	private ScripType type;
	private double strike;
	private double ltp, volume, oi;
	
}
