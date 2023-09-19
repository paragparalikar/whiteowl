package com.whiteowl.developer.indicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.whiteowl.developer.bar.Bar;

public final class Indicators {

	public static final List<Bar> bars = new ArrayList<>();
	private static final Map<String, Indicator> cache = new ConcurrentHashMap<>();
	
	private Indicators() { }
	
	public static void refresh() {
		cache.values().forEach(Indicator::refresh);
	}
	
	public static Indicator open() {
		return cache.computeIfAbsent(OpenPriceIndicator.name(), key -> new OpenPriceIndicator(bars));
	}
	
	public static Indicator high() {
		return cache.computeIfAbsent(HighPriceIndicator.name(), key -> new HighPriceIndicator(bars));
	}
	
	public static Indicator low() {
		return cache.computeIfAbsent(LowPriceIndicator.name(), key -> new LowPriceIndicator(bars));
	}
	
	public static Indicator close() {
		return cache.computeIfAbsent(ClosePriceIndicator.name(), key -> new ClosePriceIndicator(bars));
	}
	
	public static Indicator closeLocation() {
		return cache.computeIfAbsent(CloseLocationIndicator.name(), key -> new CloseLocationIndicator(bars));
	}
	
	public static Indicator typicalPrice() {
		return cache.computeIfAbsent(TypicalPriceIndicator.name(), key -> new TypicalPriceIndicator(bars));
	}
	
	public static Indicator sma(Indicator delegate, int barCount) {
		return cache.computeIfAbsent(SMAIndicator.name(delegate, barCount), key -> new SMAIndicator(delegate, barCount));
	}
	
	public static Indicator tr() {
		return cache.computeIfAbsent(TRIndicator.name(), key -> new TRIndicator(bars));
	}
	
	public static Indicator atr(int barCount) {
		return cache.computeIfAbsent(ATRIndicator.name(barCount), key -> new ATRIndicator(bars, barCount));
	}
	
	public static Indicator stochastics(int barCount) {
		return cache.computeIfAbsent(StochasticIndicator.name(barCount), key -> new StochasticIndicator(bars, barCount));
	}
}