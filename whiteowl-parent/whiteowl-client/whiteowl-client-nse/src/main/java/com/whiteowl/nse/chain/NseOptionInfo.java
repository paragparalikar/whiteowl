package com.whiteowl.nse.chain;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.scrip.ScripType;

import lombok.Data;

@Data
public class NseOptionInfo {
	private static final DateTimeFormatter FO_DATETIMEFORMATTER = DateTimeFormatter.ofPattern("ddMMM");
	private static final NumberFormat FO_NUMBERFORMAT = NumberFormat.getInstance();
	
	static {
		FO_NUMBERFORMAT.setMaximumFractionDigits(2);
		FO_NUMBERFORMAT.setMinimumFractionDigits(0);
		FO_NUMBERFORMAT.setMinimumIntegerDigits(0);
	}

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
				.build();
	}
	
	public String toScripCode(ScripType scripType) {
		return underlying 
				+ FO_DATETIMEFORMATTER.format(expiryDate)
				+ FO_NUMBERFORMAT.format(strikePrice) 
				+ scripType.name(); 
	}
	
}
