package com.whiteowl.core.strategy.test.mock;

import java.util.Optional;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public class MockOptionChainService implements OptionChainService {

	@Override
	public Optional<OptionChain> findByScrip(@NonNull final Scrip scrip) {
		throw new UnsupportedOperationException();
	}

}
