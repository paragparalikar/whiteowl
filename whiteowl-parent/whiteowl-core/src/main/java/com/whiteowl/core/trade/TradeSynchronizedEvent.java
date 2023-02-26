package com.whiteowl.core.trade;

import com.whiteowl.core.position.Position;

import lombok.Value;

@Value
public class TradeSynchronizedEvent {

	private final Trade trade;
	private final Position position;

}
