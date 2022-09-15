package com.whiteowl.ml.feature.extracter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ValueSeriesFeatureExtracter extends AbstractFeatureExtracter {
	
	@Getter
	private final String name;
	private final int offset;
	private final int barCount;
	private final BiFunction<BarSeries, Integer, Indicator<Num>> indicatorBuilder;
	
	public List<Indicator<Num>> buildIndicators(BarSeries normalSeries) {
		final List<Indicator<Num>> indicators = new ArrayList<>();
		final Indicator<Num> indicator = indicatorBuilder.apply(normalSeries, barCount);
		indicators.add(indicator);
		for(int index = 1; index < offset; index++) {
			indicators.add(new PreviousValueIndicator(indicator, index));
		}
		return indicators;
	}
	
	@Override
	public List<String> getAttributeNames() {
		final List<String> names = new ArrayList<>();
		for(int index = 0; index < offset; index++) {
			names.add("attr-" + index);
		}
		return names;
	}
	
	@Override
	public int getMinBarCount() {
		return offset + barCount + 1;
	}
}
