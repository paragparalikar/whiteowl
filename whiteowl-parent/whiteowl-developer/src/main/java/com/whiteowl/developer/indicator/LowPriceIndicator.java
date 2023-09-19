package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.bar.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LowPriceIndicator implements Indicator {
	
	public static String name() { return "low"; }

	@Getter private final List<Bar> bars;

	@Override
	public float getValue(int index) {
		return bars.get(index).low;
	}

	@Override
	public String toString() {
		return LowPriceIndicator.name();
	}
	
}
