package com.whiteowl.ml;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class BuyTradeRule extends AbstractRule {
	
	private final int barCount;
	private final BarSeries series;
	private final double targetPercentage;

	public BuyTradeRule(BarSeries series) {
		this(series, 5, 10);
	}
	
	public BuyTradeRule(BarSeries series, int barCount, double targetPercentage) {
		if(0 >= barCount) throw new IllegalArgumentException("Bar count must be positive");
		if(0 >= targetPercentage) throw new IllegalArgumentException("Target % must be positive");
		this.series = series;
		this.barCount = barCount;
		this.targetPercentage = targetPercentage;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		if(index < series.getBeginIndex() || index + barCount > series.getEndIndex()) return false;
		
		final Bar bar = series.getBar(index);
		final Bar nextBar = series.getBar(index + 1);
		final Num stopPrice = bar.getLowPrice();
		final Num entryPrice = bar.getHighPrice().plus(bar.getClosePrice()).dividedBy(DoubleNum.valueOf(2));
		if(nextBar.getLowPrice().isGreaterThanOrEqual(entryPrice)) {
			return false;
		}
		final Num targetPrice = entryPrice.plus(entryPrice.multipliedBy(
				DoubleNum.valueOf(targetPercentage)).dividedBy(DoubleNum.valueOf(100)));
		for(int i = index + 1; i < index + barCount; i++){
			final Bar futureBar = series.getBar(i);
			if(stopPrice.isGreaterThanOrEqual(futureBar.getLowPrice())) {
				return false;
			}
			if(targetPrice.isGreaterThanOrEqual(futureBar.getHighPrice())) {
				return true;
			}
		}
		return false;
	}

}
