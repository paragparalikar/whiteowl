package com.whiteowl.core.util;

import java.time.LocalDateTime;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.Num;

import com.whiteowl.core.Range;
import com.whiteowl.core.trade.Trade;

public interface Trades {
	
	public static double atrMultiple(BarSeries series) {
		return atrMultiple(series, series.getEndIndex());
	}

	public static double atrMultiple(BarSeries series, int index) {
		return atrMultiple(series, index, 8, 2);
	}
	
	public static double atrMultiple(BarSeries series, int index, int barCount, double multiple) {
		final ATRIndicator atrIndicator = new ATRIndicator(series, barCount);
		return multiple * atrIndicator.getValue(index).doubleValue();
	}
	
	public static Range getRange(int index, LocalDateTime limit, BarSeries series) {
		Num minimumPrice = Constant.ZERO, maximumPrice = Constant.ZERO;
		for(int i = index; i >= series.getBeginIndex(); i--) {
			final Bar bar = series.getBar(i);
			final LocalDateTime barTimestamp = bar.getBeginTime().toLocalDateTime();
			if(barTimestamp.isBefore(limit)) break;
			minimumPrice = bar.getLowPrice().min(minimumPrice);
			maximumPrice = bar.getHighPrice().max(maximumPrice);
		}
		return new Range(maximumPrice.doubleValue(), minimumPrice.doubleValue());
	}
	
	public static void setTimestamps(Trade trade, LocalDateTime timestamp) {
		trade.setCreatedDate(timestamp);
		trade.setExchangeTimestamp(timestamp);
		trade.setLastModifiedDate(timestamp);
		trade.setTimestamp(timestamp);
	}
	
}
