package com.whiteowl.core.prediction;

import javax.persistence.Embeddable;

import lombok.Data;

@Data
@Embeddable
public class PredictionRange {

	private Double min;
	
	private Double max;
	
}
