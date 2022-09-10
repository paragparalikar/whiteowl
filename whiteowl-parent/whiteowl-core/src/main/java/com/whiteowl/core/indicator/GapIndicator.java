package com.whiteowl.core.indicator;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

public class GapIndicator extends AbstractIndicator<Num> {

	public GapIndicator(BarSeries series) {
		super(series);
	}

	@Override
	public Num getValue(int index) {
		final BarSeries series = getBarSeries();
		if(index <= series.getBeginIndex() || index > series.getEndIndex()) return null;
		final Bar bar = series.getBar(index);
		final Bar previousBar = series.getBar(index - 1);
		return bar.getOpenPrice().minus(previousBar.getClosePrice());
	}

}
