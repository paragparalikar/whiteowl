package com.whiteowl.core.derivative.option.chain;

import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.scrip.StrikeType;

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
	
	public StrikeType getStrikeType(double spotPrice) {
		return ScripType.CE.equals(type) ? 
				(strike >= spotPrice ? StrikeType.OTM : StrikeType.ITM ) : 
				(strike <= spotPrice ? StrikeType.OTM : StrikeType.ITM );
	}
	
}
