package com.whiteowl.ml.feature;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BarSeriesNormaliser {
	private static final Num HUNDRED = DoubleNum.valueOf(100);
	
	public BarSeries normalise(BarSeries series) {
		if(null == series || series.isEmpty()) return series;
		final Bar lastBar = series.getLastBar();
		final Num lastClose = lastBar.getClosePrice();
		final Num lastVolume = lastBar.getVolume();
		final List<Bar> bars = new ArrayList<>(series.getBarCount());
		for(int index = series.getBeginIndex(); index <= series.getEndIndex(); index++) {
			bars.add(normalise(series.getBar(index), lastClose, lastVolume));
		}
		return new BaseBarSeries(bars);
	}
	
	private Bar normalise(Bar bar, Num lastClose, Num lastVolume) {
		return BaseBar.builder()
				.openPrice(getValue(bar.getOpenPrice(), lastClose))
				.highPrice(getValue(bar.getHighPrice(), lastClose))
				.lowPrice(getValue(bar.getLowPrice(), lastClose))
				.closePrice(getValue(bar.getClosePrice(), lastClose))
				.volume(getValue(bar.getVolume(), lastVolume))
				.timePeriod(bar.getTimePeriod())
				.endTime(bar.getEndTime())
				.build();
	}
	
	private Num getValue(Num value, Num refValue) {
		return value.multipliedBy(HUNDRED).dividedBy(refValue);
	}
	
}
