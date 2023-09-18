package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;

public class CloseLocationIndicator implements Indicator {

	private final float[] values;
	@Getter private final List<Bar> bars;
	
	public CloseLocationIndicator(List<Bar> bars) {
		this.bars = bars;
		this.values = new float[bars.size()];
		refresh();
	}
	
	@Override
	public void refresh() {
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
}
