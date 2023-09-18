package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class ATRIndicator extends SMAIndicator {
	
	public static String name(int barCount) { return "atr(" + barCount + ")"; }
	
	private final int barCount;
	
	public ATRIndicator(List<Bar> bars, int barCount) {
		super(new TRIndicator(bars), barCount);
		this.barCount = barCount;
	}
	
	@Override
	public String toString() {
		return ATRIndicator.name(barCount);
	}

}
