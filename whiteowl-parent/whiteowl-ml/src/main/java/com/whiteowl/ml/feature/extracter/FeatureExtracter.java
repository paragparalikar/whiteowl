package com.whiteowl.ml.feature.extracter;

import java.util.List;

import org.ta4j.core.BarSeries;

public interface FeatureExtracter {
	
	String getName();
	
	int getMinBarCount();
	
	List<String> getAttributeNames();

	List<Double> extract(int index, BarSeries series);
	
}
