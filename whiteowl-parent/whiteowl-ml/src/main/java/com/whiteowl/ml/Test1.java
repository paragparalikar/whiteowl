package com.whiteowl.ml;

import org.ta4j.core.BarSeries;

public class Test1 {

	public static void main(String[] args) {
		final TestDataService dataService = new TestDataService();
		final BarSeries barSeries = dataService.getBarSeries("NIFTY 50.csv");
		for(int index = 0; index < barSeries.getBarCount(); index++) {
			System.out.println(barSeries.getBar(index));
		}
	}

}
