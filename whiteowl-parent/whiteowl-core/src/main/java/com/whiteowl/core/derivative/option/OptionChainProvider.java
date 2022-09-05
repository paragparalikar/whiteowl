package com.whiteowl.core.derivative.option;

import java.util.Optional;

public interface OptionChainProvider {

	Optional<OptionChain> get(String underlyingCode);
	
}
