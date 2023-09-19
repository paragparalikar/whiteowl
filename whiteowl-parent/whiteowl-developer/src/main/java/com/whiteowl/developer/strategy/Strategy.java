package com.whiteowl.developer.strategy;

import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.trade.TradeSeries;

public interface Strategy {

	public int[][] getConfigs();
	
	TradeSeries execute(int[] config, BarSeries barSeries);
}
