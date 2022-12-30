package com.whiteowl.strategy.test.mock;

import java.util.Optional;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Scrip;

public class MockOptionChainService implements OptionChainService {

	@Override
	public Optional<OptionChain> findByScrip(Scrip scrip) {
		throw new UnsupportedOperationException();
	}

}
