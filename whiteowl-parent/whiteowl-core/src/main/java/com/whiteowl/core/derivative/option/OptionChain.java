package com.whiteowl.core.derivative.option;

import java.util.HashMap;
import java.util.Map;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChain {

	private final Scrip underlying;
	private final Map<Scrip, OptionChainItem> items = new HashMap<>(); 

}
