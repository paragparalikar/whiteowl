package com.whiteowl.core.rule;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LongOpportunityRule extends AbstractRule {

	private final BarSeries series;
	
	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		if(index < series.getBeginIndex() || index > series.getEndIndex() - 1) {
			return false;
		}
		
		final Bar bar = series.getBar(index);
		final Bar future = series.getBar(index + 1);
		
		final Num entryPrice = bar.getClosePrice().plus(bar.getHighPrice()).dividedBy(DoubleNum.valueOf(2));
		
		return future.getLowPrice().isLessThan(entryPrice);
	}

}
