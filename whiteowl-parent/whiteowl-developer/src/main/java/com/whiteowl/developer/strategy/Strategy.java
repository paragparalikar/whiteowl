package com.whiteowl.developer.strategy;

import java.util.List;
import java.util.Set;

import com.whiteowl.developer.trade.Trade;

public interface Strategy<T> {

	public Set<T> getConfigs();
	
	List<Trade> execute(T config);
}
