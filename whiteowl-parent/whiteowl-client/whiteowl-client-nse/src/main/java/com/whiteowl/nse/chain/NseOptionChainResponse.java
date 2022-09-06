package com.whiteowl.nse.chain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NseOptionChainResponse {

	private NseOptionChain records;
	
}
