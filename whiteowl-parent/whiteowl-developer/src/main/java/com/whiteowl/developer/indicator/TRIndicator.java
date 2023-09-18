package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class TRIndicator implements Indicator {
	
	private final float[] values;

	public TRIndicator(List<Bar> bars) {
		final int size = bars.size();
		values = new float[size];
		values[size - 1] = Float.NaN;
		for(int index = size - 2; index >= 0; index--) {
			final Bar bar = bars.get(index);
			final Bar previousBar = bars.get(index + 1);
			values[index] = Math.max(bar.high - bar.close, 
					Math.max(Math.abs(bar.high - previousBar.close), 
							Math.abs(previousBar.close - bar.low)));
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
