package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClosePriceIndicator implements Indicator {

	public static String name() { return "close"; }
	
	@Getter private final List<Bar> bars;

	@Override
	public float getValue(int index) {
		return bars.get(index).close;
	}
	
	@Override
	public String toString() {
		return ClosePriceIndicator.name();
	}

}
