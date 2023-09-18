package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClosePriceIndicator implements Indicator {

	private final List<Bar> bars;

	@Override
	public float getValue(int index) {
		return bars.get(index).close;
	}
	
	@Override
	public int getSize() {
		return bars.size();
	}

}
