package com.whiteowl.core.derivative.option.chain;

import static com.whiteowl.core.scrip.ScripType.CE;
import static com.whiteowl.core.scrip.ScripType.PE;
import static com.whiteowl.core.scrip.StrikeType.OTM;

import java.util.Set;
import java.util.function.ToDoubleFunction;

import lombok.Value;

@Value
public class OptionChainReport {

	private double pcrVolume, pcrOi, pcrPrice;
	private double pcrOtmVolume, pcrOtmOi, pcrOtmPrice;

	public OptionChainReport(OptionChain optionChain) {
		final Set<Option> puts = optionChain.getOptions(PE);
		final Set<Option> calls = optionChain.getOptions(CE);
		final Set<Option> otmCalls = optionChain.getOptions(CE, OTM);
		final Set<Option> otmPuts = optionChain.getOptions(PE, OTM);
		
		this.pcrOi = computePcr(puts, calls, Option::getOi);
		this.pcrPrice = computePcr(puts, calls, Option::getLtp);
		this.pcrVolume = computePcr(puts, calls, Option::getVolume);
		this.pcrOtmOi = computePcr(otmPuts, otmCalls, Option::getOi);
		this.pcrOtmPrice = computePcr(otmPuts, otmCalls, Option::getLtp);
		this.pcrOtmVolume = computePcr(otmPuts, otmCalls, Option::getVolume);
	}
	
	private double computePcr(Set<Option> puts, Set<Option> calls, ToDoubleFunction<? super Option> function) {
		return puts.stream().mapToDouble(function).sum() / calls.stream().mapToDouble(function).sum();
	}
	
	
}
