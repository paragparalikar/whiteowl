package com.whiteowl.core.derivative.option;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionChainItem {

	private long volume;
	private long openItnterest;
	private long changeInOpenItnterest;
	private double impliedVolatility;
	private double lastTradedPrice;
	private double change;
	private long bidQuantity;
	private double bidPrice;
	private long askQuantity;
	private long askPrice;

	private double delta;
	private double gamma;
	private double theta;
}
