package com.whiteowl.core.indicator;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class RatioIndicator extends CachedIndicator<Num> {
	
	private final Indicator<Num> numerator;
	private final Indicator<Num> denominator;

	public RatioIndicator(Indicator<Num> numerator, Indicator<Num> denominator) {
		super(numerator);
		this.numerator = numerator;
		this.denominator = denominator;
	}

	@Override
	protected Num calculate(int index) {
		final Num numeratorValue = numerator.getValue(index);
		if(null == numeratorValue || numeratorValue.isNaN()) return NaN.NaN;
		final Num denominatorValue = denominator.getValue(index);
		if(null == denominatorValue || denominatorValue.isNaN()) return NaN.NaN;
		return numeratorValue.dividedBy(denominatorValue);
	}

}
