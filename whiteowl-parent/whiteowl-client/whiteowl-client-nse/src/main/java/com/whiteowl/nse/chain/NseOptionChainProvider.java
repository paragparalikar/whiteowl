package com.whiteowl.nse.chain;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainProvider;

@Component
public class NseOptionChainProvider implements OptionChainProvider {
	
	@Override
	public Optional<OptionChain> get(String underlyingCode) {
		return null;
	}

}
