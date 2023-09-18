package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OpenPriceIndicator implements Indicator {
	
	public static String name() { return "open"; }

	@Getter private final List<Bar> bars;
	
	@Override
	public float getValue(int index) {
		return bars.get(index).open;
	}
	
	@Override
	public String toString() {
		return OpenPriceIndicator.name();
	}

}
