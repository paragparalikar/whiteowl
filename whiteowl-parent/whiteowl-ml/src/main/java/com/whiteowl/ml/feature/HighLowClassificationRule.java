package com.whiteowl.ml.feature;

import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HighLowClassificationRule implements Rule {

	private final int offset;
	private final Indicator<Num> indicator;

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		if(index + offset > indicator.getBarSeries().getEndIndex()) return false;
		final Num currentValue = indicator.getValue(index);
		final Num futureValue = indicator.getValue(index + offset);
		return futureValue.isGreaterThanOrEqual(currentValue);
	}
	
}
