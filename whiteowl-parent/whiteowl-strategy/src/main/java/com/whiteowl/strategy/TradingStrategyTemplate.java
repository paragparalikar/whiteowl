package com.whiteowl.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradingStrategyTemplate {

	SHORT_STRADDLE("short-straddle", "Short Straddle, Intraday");
	
	private final String id;
	private final String displayName;
	
}
