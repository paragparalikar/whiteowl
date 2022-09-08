package com.whiteowl.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradingStrategyTemplate {

	SHORT_STRADDLE("short-strangle", "Short Strangle");
	
	private final String id;
	private final String displayName;
	
}
