package com.whiteowl.core.derivative.option;

import java.util.concurrent.CompletableFuture;

import com.whiteowl.core.scrip.Scrip;

public interface OptionChainProvider {

	CompletableFuture<OptionChain> get(Scrip scrip);
	
}
