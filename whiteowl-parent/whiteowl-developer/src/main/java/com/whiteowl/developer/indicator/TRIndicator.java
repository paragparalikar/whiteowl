package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.bar.Bar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TRIndicator implements Indicator {
	
	public static String name() { return "tr"; }
	
	@Getter private final List<Bar> bars;
	
	@Override
	public float getValue(int index) {
		if(index == bars.size() - 1) return Float.NaN;
		final Bar bar = bars.get(index);
		final Bar previousBar = bars.get(index + 1);
		return Math.max(bar.high - bar.close, 
				Math.max(Math.abs(bar.high - previousBar.close), 
						Math.abs(previousBar.close - bar.low)));
	}

	@Override
	public String toString() {
		return TRIndicator.name();
	}
	
}
