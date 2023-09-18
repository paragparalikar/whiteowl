package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class CloseLocationIndicator implements Indicator {

	private final float[] values;
	
	public CloseLocationIndicator(List<Bar> bars) {
		values = new float[bars.size()];
		for(int index = bars.size() - 1; index >= 0; index--) {
			final Bar bar = bars.get(index);
			values[index] = bar.high == bar.low ? 0.5f : 
					(bar.close - bar.low) / (bar.high - bar.low);
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
