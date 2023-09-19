package com.whiteowl.developer.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.whiteowl.developer.bar.Bar;
import com.whiteowl.developer.indicator.Indicators;
import com.whiteowl.developer.trade.Trade;

public interface Strategy<T extends StrategyConfig> {

	Set<T> getConfigs();
	
	float enter(int index, T config);
	
	float exit(float entryPrice, long entryDate, int index, T Config);
	
	default List<Trade> execute(T config){
		long entryDate = 0;
		float entryPrice = 0, exitPrice = 0;
		final List<Trade> trades = new ArrayList<>();
		for(int index = config.getUnstablePeriod(); index < Indicators.bars.size(); index++) {
			final Bar bar = Indicators.bars.get(index);
			if(0 < entryPrice && 0 < (exitPrice = exit(entryPrice, entryDate, index, config))) {
				trades.add(new Trade(entryDate, bar.date, entryPrice, exitPrice));
				entryPrice = exitPrice = 0;
				entryDate = 0;
			} else if(0 < (entryPrice = enter(index, config))) {
				entryDate = bar.date;
			}
		}
		return trades;
	};
}
