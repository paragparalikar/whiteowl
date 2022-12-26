package com.whiteowl.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradingStrategyTemplate {

	DONCHIAN("donchian-breakout", "Donchian Channel Breakout"),
	SHORT_STRADDLE("short-straddle", "Short Straddle, Intraday"),
	MEAN_REVERSION_UPTREND("mean-reversion-uptrend", "Mean Reversion in Uptrend");
	
	private final String id;
	private final String displayName;
	
}
