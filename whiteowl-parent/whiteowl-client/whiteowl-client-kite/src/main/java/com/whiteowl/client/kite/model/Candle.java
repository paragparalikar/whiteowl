package com.whiteowl.client.kite.model;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Candle {

	private final ZonedDateTime timestamp;
	private final long volume, openInterest;
	private final double open, high, low, close;
	
}
