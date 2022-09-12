package com.whiteowl.core.prediction;

import java.time.Duration;
import java.util.Set;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

public interface PredictionProvider {
	
	void rebuild(Duration ttl);

	Set<Prediction> predict(Scrip scrip, Timeframe timeframe);
	
}
