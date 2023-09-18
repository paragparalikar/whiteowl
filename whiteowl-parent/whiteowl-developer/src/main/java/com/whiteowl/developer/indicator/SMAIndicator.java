package com.whiteowl.developer.indicator;

public class SMAIndicator implements Indicator {

	private final float[] values;
	
	public SMAIndicator(Indicator delegate, int barCount) {
		final int size = delegate.getSize();
		values = new float[size];
		float sum = 0;
		for(int index = size - 1; index >= 0; index--) {
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
	public int getSize() {
		return values.length;
	}

}
