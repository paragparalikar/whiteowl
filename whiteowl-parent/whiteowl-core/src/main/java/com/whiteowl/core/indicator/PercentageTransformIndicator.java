package com.whiteowl.core.indicator;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class PercentageTransformIndicator extends AbstractIndicator<Num> {
	private static final Num HUNDRED = DoubleNum.valueOf(100);

	private final Indicator<Num> indicator;
	
	public PercentageTransformIndicator(Indicator<Num> indicator) {
		super(indicator.getBarSeries());
		this.indicator = indicator;
	}

	@Override
	public Num getValue(int index) {
		final Num closePrice = getBarSeries().getBar(index).getClosePrice();
		final Num atr = indicator.getValue(index);
		return getValue(atr, closePrice);
	}

	private Num getValue(Num value, Num refValue) {
		return value.multipliedBy(HUNDRED).dividedBy(refValue);
	}
}
