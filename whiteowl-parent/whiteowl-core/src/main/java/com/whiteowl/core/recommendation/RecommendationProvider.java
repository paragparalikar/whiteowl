package com.whiteowl.core.recommendation;

import java.util.Set;

import com.whiteowl.core.prediction.Prediction;

public interface RecommendationProvider {

	Set<Recommendation> recommend(Prediction prediction);
	
}
