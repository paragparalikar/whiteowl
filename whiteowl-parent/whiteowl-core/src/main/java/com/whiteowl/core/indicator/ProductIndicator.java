package com.whiteowl.core.indicator;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class ProductIndicator extends CachedIndicator<Num> {

	private final Indicator<Num> first;
	private final Indicator<Num> second;

	public ProductIndicator(Indicator<Num> first, Indicator<Num> second) {
		super(first);
		this.first = first;
		this.second = second;
	}

	@Override
	protected Num calculate(int index) {
		final Num firstValue = first.getValue(index);
		if(null == firstValue || firstValue.isNaN()) return NaN.NaN;
		final Num secondValue = second.getValue(index);
		if(null == secondValue || secondValue.isNaN()) return NaN.NaN;
		return firstValue.multipliedBy(secondValue);
	}
	
}
