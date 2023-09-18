package com.whiteowl.developer.indicator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.whiteowl.developer.Bar;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Indicators {

	private final List<Bar> bars;
	private final Map<String, Indicator> cache = new ConcurrentHashMap<>();
	
	public Indicator open() {
		return cache.computeIfAbsent("open", key -> new OpenPriceIndicator(bars));
	}
	
	public Indicator high() {
		return cache.computeIfAbsent("high", key -> new HighPriceIndicator(bars));
	}
	
	public Indicator low() {
		return cache.computeIfAbsent("low", key -> new LowPriceIndicator(bars));
	}
	
	public Indicator close() {
		return cache.computeIfAbsent("close", key -> new ClosePriceIndicator(bars));
	}
	
	public Indicator closeLocation() {
		return cache.computeIfAbsent("close-location", key -> new CloseLocationIndicator(bars));
	}
	
	public Indicator typicalPrice() {
		return cache.computeIfAbsent("typical-price", key -> new TypicalPriceIndicator(bars));
	}
	
	public Indicator sma() {
		
	}
}