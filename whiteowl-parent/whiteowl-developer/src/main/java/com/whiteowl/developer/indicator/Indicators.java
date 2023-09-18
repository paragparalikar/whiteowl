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
		return cache.computeIfAbsent(OpenPriceIndicator.name(), key -> new OpenPriceIndicator(bars));
	}
	
	public Indicator high() {
		return cache.computeIfAbsent(HighPriceIndicator.name(), key -> new HighPriceIndicator(bars));
	}
	
	public Indicator low() {
		return cache.computeIfAbsent(LowPriceIndicator.name(), key -> new LowPriceIndicator(bars));
	}
	
	public Indicator close() {
		return cache.computeIfAbsent(ClosePriceIndicator.name(), key -> new ClosePriceIndicator(bars));
	}
	
	public Indicator closeLocation() {
		return cache.computeIfAbsent(CloseLocationIndicator.name(), key -> new CloseLocationIndicator(bars));
	}
	
	public Indicator typicalPrice() {
		return cache.computeIfAbsent(TypicalPriceIndicator.name(), key -> new TypicalPriceIndicator(bars));
	}
	
	public Indicator sma(Indicator delegate, int barCount) {
		return cache.computeIfAbsent(SMAIndicator.name(delegate, barCount), key -> new SMAIndicator(delegate, barCount));
	}
	
	public Indicator tr() {
		return cache.computeIfAbsent(TRIndicator.name(), key -> new TRIndicator(bars));
	}
	
	public Indicator atr(int barCount) {
		return cache.computeIfAbsent(ATRIndicator.name(barCount), key -> new ATRIndicator(bars, barCount));
	}
	
	public Indicator stochastics(int barCount) {
		return cache.computeIfAbsent(StochasticIndicator.name(barCount), key -> new StochasticIndicator(bars, barCount));
	}
	
	
}