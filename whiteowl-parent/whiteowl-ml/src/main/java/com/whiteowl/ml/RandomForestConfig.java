package com.whiteowl.ml;

import lombok.Builder;
import lombok.Value;
import weka.classifiers.trees.RandomForest;

@Value
@Builder
public class RandomForestConfig {

	private final int maxDepth;
	private final int numFeatures;
	private final int numIterations;

	public RandomForest create() {
		final RandomForest randomForest = new RandomForest();
		randomForest.setMaxDepth(maxDepth);
		randomForest.setNumFeatures(numFeatures);
		randomForest.setNumIterations(numIterations);
		return randomForest;
	}

}
