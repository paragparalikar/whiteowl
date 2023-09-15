package com.whiteowl.developer;

public class Bar {

	public final long volume, date;
	public final float open, high, low, close;
	
	public Bar(float open, float high, float low, float close, long volume, long date) {
		this.volume = volume;
		this.date = date;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
	}

}
