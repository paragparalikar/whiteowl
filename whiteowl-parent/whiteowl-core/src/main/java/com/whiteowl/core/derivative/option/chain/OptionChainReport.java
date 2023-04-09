package com.whiteowl.core.derivative.option.chain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChainReport {

	private double supportStrengthRatio, resistanceStrengthRatio;
	private double eosDistance, eorDistance; // in percentage
	private double pcrVolume, pcrOi, pcrPrice;

}
