package com.whiteowl.client.kite.model;

import java.util.Date;

import lombok.Data;

@Data
public class KiteQuote {

	private long instrumentToken;
	private Date timestamp;
	private Date lastTradeTime;
	private double lastPrice;
	private int lastQuantity;
	private int buyQuantity;
	private int sellQuantity;
	private long volume;
	private double averagePrice;
	private double oi;
	private double oiDayHigh;
	private double oiDayLow;
	private KiteOhlc ohlc;
	private double netChange;
	private double lowerCircuitLimit;
	private double upperCircuitLimit;
	private KiteMarketDepth depth;
	
}
