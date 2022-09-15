package com.whiteowl.ml.feature.extracter;

import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

public abstract class AbstractFeatureExtracter implements FeatureExtracter {

	protected abstract List<Indicator<Num>> buildIndicators(BarSeries series); 

	@Override
	public List<Double> extract(int index, BarSeries series){
		final List<Indicator<Num>> indicators = buildIndicators(series);
		return indicators.stream()
				.map(indicator -> indicator.getValue(index))
				.map(Num::doubleValue)
				.collect(Collectors.toList());
	}
	
	protected Indicator<Num> differenceIndicator(Indicator<Num> indicator, int smaBarCount){
		return new DifferenceIndicator(indicator, new PreviousValueIndicator(new SMAIndicator(indicator, smaBarCount)));
	}

}
