package com.whiteowl.core.derivative.option;

import java.util.Optional;

import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public interface OptionChainService {

	Optional<OptionChain> findByScrip(Scrip scrip);

}