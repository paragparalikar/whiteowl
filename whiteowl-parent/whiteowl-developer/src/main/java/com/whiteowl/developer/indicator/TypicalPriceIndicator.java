package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class TypicalPriceIndicator implements Indicator {

	private final float[] values;
	
	public TypicalPriceIndicator(List<Bar> bars) {
		values = new float[bars.size()];
		for(int index = bars.size() - 1; index >= 0; index--) {
			final Bar bar = bars.get(index);
			values[index] = (bar.high + bar.close + bar.low) / 3;
		}
	}
	
	@Override
	public float getValue(int index) {
		return values[index];
	}
	
	@Override
	public int getSize() {
		return values.length;
	}

}
