package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

public class ATRIndicator extends SMAIndicator {
	
	public ATRIndicator(List<Bar> bars, int barCount) {
		super(new TRIndicator(bars), barCount);
	}

}
