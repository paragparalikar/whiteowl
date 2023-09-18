package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;

public class StochasticIndicator implements Indicator {
	
	public static String name(int barCount) { return "stochastics(" + barCount + ")"; }
	
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
		for(int index = 0; index < size; index++) {
			if(index < barCount - 1) {
				values[index] = Float.NaN;
			} else {
				float highest = Float.MIN_VALUE, lowest = Float.MAX_VALUE;
				for(int j = index - barCount + 1; j <= index; j++) {
					final Bar bar = bars.get(j);
					highest = highest >= bar.high ? highest : bar.high;
					lowest = lowest <= bar.low ? lowest : bar.low;
				}
				values[index] = (bars.get(index).close - lowest) * 100 / (highest - lowest);
			}
		}
	}

	@Override
	public float getValue(int index) {
		return values[index];
	}
	
	@Override
	public String toString() {
		return StochasticIndicator.name(barCount);
	}
	
}
