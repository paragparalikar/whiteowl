package com.whiteowl.core.analysis.walkForward;

import java.io.Serializable;

import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WalkForwardStep implements Serializable {
	private static final long serialVersionUID = 737799451055485885L;

	private int trainBarCount, testBarCount;
	private TradingStrategyConfig selectedConfig;
	private TradingStrategyConfigPerformance trainPerformance, testPerformance;

	public double getEfficiencyByCagr() {
		return testPerformance.getCagr() / Math.max(0.0001, trainPerformance.getCagr());
	}
	
	public double getEfficiencyByCagrOverAvgDrawdown() {
		return testPerformance.getCagrOverAvgDrawdown() / Math.max(0.0001, trainPerformance.getCagrOverAvgDrawdown());
	}
	
	public double getEfficiencyByCagrOverAvgDrawdownAndExposure() {
		return testPerformance.getCagrOverAvgDrawdownAndExposure() / 
				Math.max(0.0001, trainPerformance.getCagrOverAvgDrawdownAndExposure());
	}
}
