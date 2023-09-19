package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.bar.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HighPriceIndicator implements Indicator {
	
	public static String name() { return "high"; }

	@Getter private final List<Bar> bars;

	@Override
	public float getValue(int index) {
		return bars.get(index).high;
	}
	
	@Override
	public String toString() {
		return HighPriceIndicator.name();
	}

}
