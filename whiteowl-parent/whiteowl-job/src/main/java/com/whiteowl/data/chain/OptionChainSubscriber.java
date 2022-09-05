package com.whiteowl.data.chain;

import com.whiteowl.core.derivative.option.OptionChain;

public interface OptionChainSubscriber {

	void onOptionChain(OptionChain optionChain);
	
}
