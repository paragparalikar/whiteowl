package com.whiteowl.core.prediction;

import java.util.Set;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

public interface PredictionProvider {

	Set<Prediction> predict(Scrip scrip, Timeframe timeframe);
	
}
