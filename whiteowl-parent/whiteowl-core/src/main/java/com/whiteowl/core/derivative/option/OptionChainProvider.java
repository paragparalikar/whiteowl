package com.whiteowl.core.derivative.option;

import java.util.Optional;

import com.whiteowl.core.scrip.Scrip;

public interface OptionChainProvider {

	Optional<OptionChain> get(Scrip scrip);
	
}
