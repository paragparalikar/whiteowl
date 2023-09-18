package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypicalPriceIndicator implements Indicator {
	
	public static String name() { return "tp"; }

	@Getter private final List<Bar> bars;

	@Override
	public float getValue(int index) {
		final Bar bar = bars.get(index);
		return (bar.high + bar.close + bar.low) / 3;
	}

	@Override
	public String toString() {
		return TypicalPriceIndicator.name();
	}
	
}
