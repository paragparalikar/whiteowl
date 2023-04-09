package com.whiteowl.core.derivative.option.chain;

import static com.whiteowl.core.scrip.ScripType.CE;
import static com.whiteowl.core.scrip.ScripType.PE;
import static com.whiteowl.core.scrip.StrikeType.ITM;
import static com.whiteowl.core.scrip.StrikeType.OTM;

import java.util.Set;
import java.util.function.ToDoubleFunction;

import lombok.Value;

@Value
public class OptionChainReport {

	private double pcrVolume, pcrOi, pcrPrice;
	private double pcrOtmVolume, pcrOtmOi, pcrOtmPrice;
	private double callOiRatio, callVolumeRatio, putOiRatio, putVolumeRatio;

	public OptionChainReport(OptionChain optionChain) {
		final Set<Option> puts = optionChain.getOptions(PE);
		final Set<Option> calls = optionChain.getOptions(CE);
		final Set<Option> otmCalls = optionChain.getOptions(CE, OTM);
		final Set<Option> otmPuts = optionChain.getOptions(PE, OTM);
		final Set<Option> itmCalls = optionChain.getOptions(CE, ITM);
		final Set<Option> itmPuts = optionChain.getOptions(PE, ITM);
		
		this.pcrOi = computeRatio(puts, calls, Option::getOi);
		this.pcrPrice = computeRatio(puts, calls, Option::getLtp);
		this.pcrVolume = computeRatio(puts, calls, Option::getVolume);
		this.pcrOtmOi = computeRatio(otmPuts, otmCalls, Option::getOi);
		this.pcrOtmPrice = computeRatio(otmPuts, otmCalls, Option::getLtp);
		this.pcrOtmVolume = computeRatio(otmPuts, otmCalls, Option::getVolume);
		this.callOiRatio = computeRatio(otmCalls, itmCalls, Option::getOi);
		this.callVolumeRatio = computeRatio(otmCalls, itmCalls, Option::getVolume);
		this.putOiRatio = computeRatio(otmPuts, itmPuts, Option::getOi);
		this.putVolumeRatio = computeRatio(otmPuts, itmPuts, Option::getVolume);
	}
	
	private double computeRatio(Set<Option> numerators, Set<Option> denominators, ToDoubleFunction<? super Option> function) {
		return numerators.stream().mapToDouble(function).sum() / denominators.stream().mapToDouble(function).sum();
	}
	
}
