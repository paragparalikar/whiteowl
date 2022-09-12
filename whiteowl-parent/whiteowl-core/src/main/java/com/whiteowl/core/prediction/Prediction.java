package com.whiteowl.core.prediction;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.whiteowl.core.scrip.Scrip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {

	@Id
	@GeneratedValue
	private Long id;
	
	private String modelId;
	
	@ManyToOne(optional = false)
	private Scrip scrip;
	
	@Embedded
	private PredictionRange high;
	
	@Embedded
	private PredictionRange low;
	
	@Embedded
	private PredictionRange close;
	
}
