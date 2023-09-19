package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.bar.Bar;

public class SMAIndicator implements Indicator {
	
	public static String name(Indicator delegate, int barCount) { 
		return "sma(" + delegate + "," + barCount + ")"; 
	}

	private final int barCount;
	private volatile float[] values;
	private final Indicator delegate;
	
	public SMAIndicator(Indicator delegate, int barCount) {
		this.barCount = barCount;
		this.delegate = delegate;
		this.values = new float[delegate.getBars().size()];
		refresh();
	}
	
	@Override
	public void refresh() {
		float sum = 0;
		final int size = delegate.getBars().size();
		if(values.length != size) {
			this.values = new float[size];
		}
		for(int index = 0; index < size; index++) {
			 sum += delegate.getValue(index);
			 if(index > size - barCount) {
				values[index] = Float.NaN;
			} else {
				if(index < size - barCount) {
					sum -= delegate.getValue(index + barCount);
				}
				values[index] = sum / barCount;
			}
		}
	}

	@Override
	public float getValue(int index) {
		return values[index];
	}
	
	@Override
	public List<Bar> getBars() {
		return delegate.getBars();
	}
	
	@Override
	public String toString() {
		return SMAIndicator.name(delegate, barCount);
	}

}
