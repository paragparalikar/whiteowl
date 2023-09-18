package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;

public class StochasticIndicator implements Indicator {
	
	private final int barCount;
	private final float[] values;
	@Getter private final List<Bar> bars;

	public StochasticIndicator(List<Bar> bars, int barCount) {
		this.bars = bars;
		this.barCount = barCount;
		this.values = new float[bars.size()];
		refresh();
	}
	
	@Override
	public void refresh() {
		final int size = bars.size();
		for(int index = size - 1; index >= 0; index--) {
			final int start = index + barCount - 1;
			float highest = Float.MIN_VALUE, lowest = Float.MAX_VALUE;
			for(int j = start >= size ? size - 1 : start; j >= index; j--) {
				final Bar bar = bars.get(j);
				highest = highest >= bar.high ? highest : bar.high;
				lowest = lowest <= bar.low ? lowest : bar.low;
			}
			if(index > size - barCount) {
				values[index] = Float.NaN;
			} else {
				values[index] = (bars.get(index).close - lowest) * 100 / (highest - lowest);
			}
		}
	}

	@Override
	public float getValue(int index) {
		return values[index];
	}
	
}
