package com.whiteowl.ml.feature;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

import lombok.Getter;
@Getter
public class LongSuccessClassificationRule extends AbstractRule {
	
	private final int barCount;
	private final BarSeries series;
	private final double targetPercentage;
	private final double stopLossPercentage;
	
	public LongSuccessClassificationRule(BarSeries series, int barCount, double targetPercentage, double stopLossPercentage) {
		if(0 >= barCount) throw new IllegalArgumentException("Bar count must be positive");
		if(0 >= targetPercentage) throw new IllegalArgumentException("Target % must be positive");
		this.series = series;
		this.barCount = barCount;
		this.targetPercentage = targetPercentage;
		this.stopLossPercentage = stopLossPercentage;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		if(index < series.getBeginIndex() || index + barCount > series.getEndIndex()) return false;
		final Bar bar = series.getBar(index);
		final Num entryPrice = bar.getHighPrice().plus(bar.getClosePrice()).dividedBy(DoubleNum.valueOf(2));
		
		  final Num stopPrice = entryPrice.minus(entryPrice.multipliedBy(
		  DoubleNum.valueOf(stopLossPercentage)).dividedBy(DoubleNum.valueOf(100)));
		 
		//final Num stopPrice = bar.getLowPrice();
		final Num targetPrice = entryPrice.plus(entryPrice.multipliedBy(
				DoubleNum.valueOf(targetPercentage)).dividedBy(DoubleNum.valueOf(100)));
		for(int i = index + 1; i < index + barCount; i++){
			final Bar futureBar = series.getBar(i);
			if(stopPrice.isGreaterThanOrEqual(futureBar.getLowPrice())) {
				return false;
			}
			if(targetPrice.isLessThan(futureBar.getHighPrice())) {
				return true;
			}
		}
		return false;
	}

}
