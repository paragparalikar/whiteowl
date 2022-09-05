package com.whiteowl.core.derivative.option;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChain {

	private Scrip scrip;

}
