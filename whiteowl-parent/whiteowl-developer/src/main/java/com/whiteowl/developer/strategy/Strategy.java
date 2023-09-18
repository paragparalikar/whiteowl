package com.whiteowl.developer.strategy;

import com.whiteowl.developer.TradeSeries;

public interface Strategy {

	public int[] getConfigLengths();
	
	void execute(int[] configIndices, TradeSeries tradeSeries);
}
