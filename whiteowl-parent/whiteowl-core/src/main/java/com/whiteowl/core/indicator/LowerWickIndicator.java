package com.whiteowl.core.indicator;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

public class LowerWickIndicator extends AbstractIndicator<Num> {

	public LowerWickIndicator(BarSeries series) {
		super(series);
	}

	@Override
	public Num getValue(int index) {
		final BarSeries series = getBarSeries();
		if(index < series.getBeginIndex() || index > series.getEndIndex()) return null;
		final Bar bar = series.getBar(index);
		return bar.getClosePrice().min(bar.getOpenPrice()).min(bar.getLowPrice());
	}

}
