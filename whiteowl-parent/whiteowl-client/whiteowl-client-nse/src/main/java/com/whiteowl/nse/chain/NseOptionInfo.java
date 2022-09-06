package com.whiteowl.nse.chain;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.util.BlackScholes;

import lombok.Data;

@Data
public abstract class NseOptionInfo {
	private static final DateFormat FO_DATETIMEFORMATTER = new SimpleDateFormat("ddMMM");
	private static final NumberFormat FO_NUMBERFORMAT = NumberFormat.getInstance();
	
	static {
		FO_NUMBERFORMAT.setMaximumFractionDigits(2);
		FO_NUMBERFORMAT.setMinimumFractionDigits(0);
		FO_NUMBERFORMAT.setMinimumIntegerDigits(0);
		FO_NUMBERFORMAT.setGroupingUsed(false);
	}

	private final ScripType scripType;
	private double strikePrice;
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy")
	private Date expiryDate;
	private String underlying;
	private String identifier;
	private long openInterest;
	private long changeinOpenInterest;
	private double pchangeinOpenInterest;
	private long totalTradedVolume;
	private double impliedVolatility;
	private double lastPrice;
	private double change;
	@JsonProperty("pChange")
	private double changePercentage;
	private long totalBuyQuantity;
	private long totalSellQuantity;
	private long bidQty;
	private double bidprice;
	private long askQty;
	private double askPrice;
	private double underlyingValue;
	
	public OptionChainItem toOptionChainItem() {
		double delta = 0, gamma = 0, theta = 0, vega = 0;
		if(0 < totalTradedVolume) {
			final long diff = expiryDate.getTime() - System.currentTimeMillis();
			final BlackScholes blackScholes = new BlackScholes(underlyingValue, strikePrice, 7, 
					impliedVolatility, Duration.ofMillis(diff).toDays());
			vega = blackScholes.getVega();
			gamma = blackScholes.getGamma();
			theta = ScripType.CE.equals(scripType) ? blackScholes.getCallTheta() : blackScholes.getPutTheta();
			delta = ScripType.CE.equals(scripType) ? blackScholes.getCallDelta() : blackScholes.getPutDelta();
		}
		return OptionChainItem.builder()
				.volume(totalTradedVolume)
				.openItnterest(openInterest)
				.changeInOpenItnterest(changeinOpenInterest)
				.changeInOpenInterestPercentage(pchangeinOpenInterest)
				.impliedVolatility(impliedVolatility)
				.lastTradedPrice(lastPrice)
				.change(change)
				.changePercentage(changePercentage)
				.underlyingValue(underlyingValue)
				.bidQuantity(bidQty)
				.bidPrice(bidprice)
				.askQuantity(askQty)
				.askPrice(askPrice)
				.delta(delta)
				.gamma(gamma)
				.theta(theta)
				.vega(vega)
				.build();
	}
	
	public String toScripCode() {
		return (underlying.toUpperCase() 
				+ FO_DATETIMEFORMATTER.format(expiryDate)
				+ FO_NUMBERFORMAT.format(strikePrice) 
				+ scripType.name()).toUpperCase(); 
	}
	
}
