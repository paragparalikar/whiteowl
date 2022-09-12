package com.whiteowl.core.recommendation;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultRecommendationService implements RecommendationService{

	private final RecommendationRepository recommendationRepository;
	
}
