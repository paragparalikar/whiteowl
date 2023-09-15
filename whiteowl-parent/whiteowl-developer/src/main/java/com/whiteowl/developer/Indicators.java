package com.whiteowl.developer;

import java.util.List;

public interface Indicators {

	public static float[] stochastics(List<Bar> bars, int barCount) {
		final int size = bars.size();
		final float[] values = new float[size];
		for(int index = size - 1; index >= 0; index--) {
			final int start = index + barCount - 1;
			float highest = Float.MIN_VALUE, lowest = Float.MAX_VALUE;
			for(int j = start >= size ? size - 1 : start; j >= index; j--) {
				final Bar bar = bars.get(j);
				highest = highest >= bar.high ? highest : bar.high;
				lowest = lowest <= bar.low ? lowest : bar.low;
			}
			if(index > size - barCount) {
				values[index] = Float.MIN_VALUE;
			} else {
				values[index] = (bars.get(index).close - lowest) * 100 / (highest - lowest);
			}
		}
		return values;
	}
	
	public static float[] sma(List<Bar> bars, int barCount) {
		final int size = bars.size();
		final float[] values = new float[size];
		float sum = 0;
		for(int index = size - 1; index >= 0; index--) {
			 sum += bars.get(index).close;
			 if(index > size - barCount) {
				values[index] = Float.MIN_VALUE;
			} else {
				if(index < size - barCount) {
					sum -= bars.get(index + barCount).close;
				}
				values[index] = sum / barCount;
			}
		}
		return values;
	}
	
}
