package com.whiteowl.core.indicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class PercentATRIndicator extends AbstractIndicator<Num> {
	private static final Num HUNDRED = DoubleNum.valueOf(100);

	private final ATRIndicator atrIndicator;
	
	public PercentATRIndicator(BarSeries series, int barCount) {
		super(series);
		this.atrIndicator = new ATRIndicator(series, barCount);
	}

	@Override
	public Num getValue(int index) {
		final Num closePrice = getBarSeries().getBar(index).getClosePrice();
		final Num atr = atrIndicator.getValue(index);
		return getValue(atr, closePrice);
	}

	private Num getValue(Num value, Num refValue) {
		return value.multipliedBy(HUNDRED).dividedBy(refValue);
	}
	
}
