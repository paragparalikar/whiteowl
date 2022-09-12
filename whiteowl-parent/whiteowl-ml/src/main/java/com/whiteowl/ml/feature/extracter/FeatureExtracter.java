package com.whiteowl.ml.feature.extracter;

import java.util.List;

public interface FeatureExtracter {
	
	String getName();
	
	int getMinBarCount();
	
	List<String> getAttributeNames();

	List<Double> extract(int index);
	
}
