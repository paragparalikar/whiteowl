package com.whiteowl.ml.feature.extracter;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

import lombok.Getter;

public class ValueSeriesFeatureExtracter extends AbstractFeatureExtracter {
	
	@Getter
	private final String name;
	private final int offset;

	public ValueSeriesFeatureExtracter(String name, Indicator<Num> indicator, int offset) {
		this.name = name;
		this.offset = offset;
		add(indicator);
		for(int index = 1; index < offset; index++) {
			add(new PreviousValueIndicator(indicator, index));
		}
	}
	
	@Override
	public int getMinBarCount() {
		return offset + 1;
	}
}
