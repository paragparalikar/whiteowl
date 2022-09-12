package com.whiteowl.ml.feature.extracter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

public abstract class AbstractFeatureExtracter implements FeatureExtracter {

	private final List<Indicator<Num>> indicators = new LinkedList<>();
	
	protected void add(Indicator<Num> indicator) {
		indicators.add(indicator);
	}

	public List<String> getAttributeNames(){
		final List<String> attributeNames = new ArrayList<>(indicators.size());
		for(int index = 0; index < indicators.size(); index++) {
			attributeNames.add("attr-" + index);
		}
		return attributeNames;
	}

	@Override
	public List<Double> extract(int index){
		return indicators.stream()
				.map(indicator -> indicator.getValue(index))
				.map(Num::doubleValue)
				.collect(Collectors.toList());
	}
	
	protected Indicator<Num> differenceIndicator(Indicator<Num> indicator, int smaBarCount){
		return new DifferenceIndicator(indicator, new PreviousValueIndicator(new SMAIndicator(indicator, smaBarCount)));
	}

}
