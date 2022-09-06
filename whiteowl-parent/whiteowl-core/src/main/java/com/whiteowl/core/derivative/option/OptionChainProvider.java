package com.whiteowl.core.derivative.option;

import com.whiteowl.core.scrip.Scrip;

public interface OptionChainProvider {

	OptionChain get(Scrip scrip);
	
}
