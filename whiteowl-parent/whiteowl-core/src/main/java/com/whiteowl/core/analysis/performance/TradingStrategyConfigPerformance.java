package com.whiteowl.core.analysis.performance;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class TradingStrategyConfigPerformance {

	@Id
	private String id;
	
	private Double annualReturnsPct;
	
	private Double accuracy;
	
}
