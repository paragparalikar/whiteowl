package com.whiteowl.nse.chain;

import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.whiteowl.core.scrip.ScripType;

import lombok.Data;

@Data
public class NseOptionChainItem {

	private double strikePrice;
	
	private LocalDate expiryDate;
	
	@JsonProperty("PE")
	private NseOptionInfo putOptionInfo;
	
	@JsonProperty("CE")
	private NseOptionInfo callOptionInfo;
	
	public Optional<String> getCallOptionScripCode() {
		return Optional.ofNullable(callOptionInfo)
				.map(info -> info.toScripCode(ScripType.CE));
	}
	
	public Optional<String> getPutOptionScripCode(){
		return Optional.ofNullable(putOptionInfo)
				.map(info -> info.toScripCode(ScripType.PE));
	}
	
}
