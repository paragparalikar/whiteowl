package com.whiteowl.core.indicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class FutureValuePercentageChangeIndicator extends AbstractIndicator<Num> {

	private final int offset;
	private final Indicator<Num> indicator;
	
	public FutureValuePercentageChangeIndicator(Indicator<Num> indicator, int offset) {
		super(indicator.getBarSeries());
		this.offset = offset;
		this.indicator = indicator;
	}

	@Override
	public Num getValue(int index) {
		final BarSeries series = getBarSeries();
		if(index + offset > series.getEndIndex()) {
			return DoubleNum.valueOf(0);
		} else {
			final Num currentValue = indicator.getValue(index);
			final Num futureValue = indicator.getValue(index + offset);
			return futureValue.minus(currentValue).multipliedBy(DoubleNum.valueOf(100)).dividedBy(currentValue);
		}
	}

}
