package com.whiteowl.core.trade;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TradeSynchronizedEvent {

	private final Trade trade;
	private final Position position;
	private final Portfolio portfolio;

}
