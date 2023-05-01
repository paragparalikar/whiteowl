package com.whiteowl.core.strategy.config.performance;

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
