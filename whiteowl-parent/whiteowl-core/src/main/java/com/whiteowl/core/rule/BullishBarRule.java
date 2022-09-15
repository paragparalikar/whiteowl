package com.whiteowl.core.rule;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BullishBarRule extends AbstractRule {

	private final BarSeries series;
	
	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		if(index < series.getBeginIndex() + 2 || index > series.getEndIndex()) {
			return false;
		}
		
		final Bar bar = series.getBar(index);
		final Bar one = series.getBar(index - 1);
		final Bar two = series.getBar(index - 2);
		
		final Num midPrice = bar.getHighPrice().plus(bar.getLowPrice()).dividedBy(DoubleNum.valueOf(2));
		if(bar.getClosePrice().isLessThan(midPrice)) {
			return false;
		}
		
		if(bar.getClosePrice().isLessThan(one.getHighPrice())) {
			return false;
		}
		
		if(one.getClosePrice().isGreaterThan(two.getHighPrice())) {
			return false;
		}
		
		return true;
	}

}
