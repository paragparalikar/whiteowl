package com.whiteowl.core.quote;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

	private String code;
	private double lastPrice;
	private int lastQuantity;
	private int buyQuantity;
	private int sellQuantity;
	private long volume;
	private double averagePrice;
	private double oi;
	private double oiDayHigh;
	private double oiDayLow;
	private Ohlc ohlc;
	private double netChange;
	private double lowerCircuitLimit;
	private double upperCircuitLimit;
	private MarketDepth depth;
	private LocalDateTime timestamp;
	private LocalDateTime lastTradeTime;
	
}
