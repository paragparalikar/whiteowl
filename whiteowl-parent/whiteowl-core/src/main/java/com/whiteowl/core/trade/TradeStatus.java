package com.whiteowl.core.trade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeStatus {

	NEW(false, true), 
	PENDING(false, false), 
	OPEN(false, false), 
	UPDATABLE(false, true),
	CANCELLABLE(false, true),
	CANCELLED(true, false), 
	REJECTED(true, false), 
	COMPLETE(true, false);
	
	private final boolean terminal;
	private final boolean actionable;
	
}
