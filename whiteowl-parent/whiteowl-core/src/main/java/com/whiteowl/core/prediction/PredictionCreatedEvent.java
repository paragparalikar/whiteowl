package com.whiteowl.core.prediction;

import lombok.Value;

@Value
public class PredictionCreatedEvent {
	
	private final Prediction prediction;
	
}
