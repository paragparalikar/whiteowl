package com.whiteowl.core.prediction;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultPredictionService implements PredictionService {

	private final PredictionRepository predictionRepository;
	
	@Override
	public Prediction save(Prediction prediction) {
		return predictionRepository.saveAndFlush(prediction);
	}

}
