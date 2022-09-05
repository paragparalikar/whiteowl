package com.whiteowl.data.chain.nse;

import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.scrip.Scrip;

@Component
public class NseOptionChainProvider implements OptionChainProvider {

	
	
	@Override
	public OptionChain get(Scrip scrip) {
		return null;
	}

}
