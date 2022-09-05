package com.whiteowl.nse.chain;

import java.time.LocalDate;

import lombok.Data;

@Data
public class NseOptionInfo {

	private double strikePrice;
	private LocalDate expiryDate;
	private String underlying;
	private String identifier;
	private long openInterest;
	private long changeinOpenInterest;
	private double pchangeinOpenInterest;
	private long totalTradedVolume;
	private double impliedVolatility;
	private double lastPrice;
	private double change;
	private double pChange;
	private long totalBuyQuantity;
	private long totalSellQuantity;
	private long bidQty;
	private double bidprice;
	private long askQty;
	private double askPrice;
	private double underlyingValue;
	
}
