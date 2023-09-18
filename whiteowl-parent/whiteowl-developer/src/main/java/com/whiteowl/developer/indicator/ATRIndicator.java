package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class ATRIndicator implements Indicator {

	private final float[] values;
	
	public ATRIndicator(List<Bar> bars, int barCount) {
		final int size = bars.size();
		values = new float[size];
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
