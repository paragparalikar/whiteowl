package com.whiteowl.developer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.whiteowl.developer.bar.Bar;
import com.whiteowl.developer.bar.BarRepository;
import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.bar.Timeframe;
import com.whiteowl.developer.indicator.Indicators;
import com.whiteowl.developer.strategy.Strategy;

public class Main {
	private static final int LIMIT = 1000;
	
	public static void main(String[] args) {
		final List<Bar> bars = new ArrayList<>(LIMIT);
		final BarRepository barRepository = BarRepository.getInstance();
		final Indicators indicators = new Indicators(bars);
		final Strategy strategy = null;
		
		for(Timeframe timeframe : Timeframe.values()) {
			for(String code : barRepository.findAllCodes()) {
				final BarSeries series = new BarSeries(code, timeframe);
				barRepository.load(series, LIMIT * timeframe.getDayMultiple(), bars);
				indicators.refresh();
				final CountDownLatch latch = new CountDownLatch(0);
				
				
				
			}
		}
	}

}
