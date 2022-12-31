package com.whiteowl.core.derivative.option;

import java.util.Optional;

import com.whiteowl.core.scrip.Scrip;

public interface OptionChainService {

	Optional<OptionChain> findByScrip(Scrip scrip);

}