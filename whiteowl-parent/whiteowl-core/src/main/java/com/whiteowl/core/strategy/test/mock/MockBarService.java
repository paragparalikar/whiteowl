package com.whiteowl.core.strategy.test.mock;

import java.util.Collection;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockBarService implements BarService {
	
	@NonNull private final BarSeries originalBarSeries;
	@NonNull private final BarSeries barSeries = new BaseBarSeries("", DoubleNum::valueOf);
	
	public boolean next() {
		if(originalBarSeries.getEndIndex() > barSeries.getEndIndex()) {
			barSeries.addBar(originalBarSeries.getBar(barSeries.getEndIndex() + 1));
			return true;
		}
		return false;
	}
	
	@Override
	public BarSeries findLatestByCodeAndTimeframeOrderByBeginTimeAsc(String code, Timeframe timeframe, int count) {
		return barSeries;
	}

	@Override
	public void saveAll(
			@NonNull final String code, @NonNull final Timeframe timeframe, @NonNull final Collection<Bar> bars) {
		throw new UnsupportedOperationException();
	}
	
}
