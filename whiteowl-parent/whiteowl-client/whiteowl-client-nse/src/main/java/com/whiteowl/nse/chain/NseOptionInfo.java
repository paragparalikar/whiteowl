package com.whiteowl.nse.chain;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.util.BlackScholes;

import lombok.Data;

@Data
public abstract class NseOptionInfo {
	private static final DateTimeFormatter FO_DATETIMEFORMATTER = DateTimeFormatter.ofPattern("ddMMM");
	private static final NumberFormat FO_NUMBERFORMAT = NumberFormat.getInstance();
	
	static {
		FO_NUMBERFORMAT.setMaximumFractionDigits(2);
		FO_NUMBERFORMAT.setMinimumFractionDigits(0);
		FO_NUMBERFORMAT.setMinimumIntegerDigits(0);
	}

	private final ScripType scripType;
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
	
	public OptionChainItem toOptionChainItem() {
		double delta = 0, gamma = 0, theta = 0, vega = 0;
		if(0 < totalTradedVolume) {
			final BlackScholes blackScholes = new BlackScholes(underlyingValue, strikePrice, 7, 
					impliedVolatility, Duration.between(LocalDate.now(), expiryDate).toDays());
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
				.changePercentage(pChange)
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
		return underlying.toUpperCase() 
				+ FO_DATETIMEFORMATTER.format(expiryDate)
				+ FO_NUMBERFORMAT.format(strikePrice) 
				+ scripType.name(); 
	}
	
}
