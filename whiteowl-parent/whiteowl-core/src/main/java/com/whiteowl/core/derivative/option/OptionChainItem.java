package com.whiteowl.core.derivative.option;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChainItem {

	private long volume;
	private long openItnterest;
	private long changeInOpenItnterest;
	private double changeInOpenInterestPercentage;
	private double impliedVolatility;
	private double lastTradedPrice;
	private double change;
	private double changePercentage;
	private double underlyingValue;
	private long bidQuantity;
	private double bidPrice;
	private long askQuantity;
	private double askPrice;

	private double delta;
	private double gamma;
	private double theta;
	private double vega;
}
