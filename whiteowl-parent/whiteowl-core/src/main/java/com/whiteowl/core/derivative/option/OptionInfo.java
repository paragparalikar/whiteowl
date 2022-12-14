package com.whiteowl.core.derivative.option;

import com.whiteowl.core.scrip.Scrip;

import lombok.Data;

@Data
public class OptionInfo {

	private Scrip scrip;
	private long openInterest;
	private long changeinOpenInterest;
	private double changeInOpenInterestPercentage;
	private long totalTradedVolume;
	private double impliedVolatility;
	private double lastPrice;
	private double change;
	private double changePercentage;
	private double underlyingValue;
	private long totalBuyQuantity;
	private long totalSellQuantity;
	private long bidQuantity;
	private double bidPrice;
	private long askQuantity;
	private double askPrice;
	private double delta;
	private double gamma;
	private double theta;
	private double vega;

}
