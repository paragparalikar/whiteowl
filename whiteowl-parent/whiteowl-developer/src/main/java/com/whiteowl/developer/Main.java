package com.whiteowl.developer;

import java.util.ArrayList;
import java.util.List;

import com.whiteowl.developer.indicator.Indicators;
import com.whiteowl.developer.strategy.Strategy;

public class Main {
	private static final int LIMIT = 1000;

	public static void main(String[] args) {
		final List<Bar> bars = new ArrayList<>(LIMIT);
		final BarRepository barRepository = BarRepository.getInstance();
		final Indicators indicators = new Indicators(bars);
		final Strategy strategy = null;
		final int configSize = size(strategy.getConfigLengths());
		for(Timeframe timeframe : Timeframe.values()) {
			for(String code : barRepository.findAllCodes()) {
				final Series series = new Series(code, timeframe);
				barRepository.load(series, LIMIT, bars);
				indicators.refresh();
				
				
				
			}
		}
	}
	
	private static int size(int[] configLengths) {
		int product = 1;
		for(int index = 0; index < configLengths.length; index++) {
			product *= configLengths[index];
		}
		return product;
	}

}
