package com.whiteowl.developer.indicator;

import java.util.List;

import com.whiteowl.developer.bar.Bar;

public interface Indicator {
	
	List<Bar> getBars();
	
	default void refresh() {};
	
	float getValue(int index);
	
}
