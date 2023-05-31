package com.whiteowl.core.analysis.optimiser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;

import lombok.Getter;

@Getter
public class WalkForwardReport implements Consumer<WalkForwardStep>, Serializable {
	private static final long serialVersionUID = -3090614699412868090L;

	private final TradingStrategyConfigPerformance performance;
	private final List<WalkForwardStep> steps = new ArrayList<>();
	private double efficiencyByCagr, efficiencyByCagrOverAvgDrawdown, efficiencyByCagrOverAvgDrawdownAndExposure;
			
	public WalkForwardReport(double initialMargin) {
		this.performance = new TradingStrategyConfigPerformance(initialMargin);
	}
	
	@Override
	public void accept(WalkForwardStep step) {
		steps.add(step);
		step.getTestPerformance().getPositions().forEach(performance::onExit);
		efficiencyByCagr = average(this::getEfficiencyByCagr, step.getEfficiencyByCagr());
		efficiencyByCagrOverAvgDrawdown = average(this::getEfficiencyByCagrOverAvgDrawdown, step.getEfficiencyByCagrOverAvgDrawdown());
		efficiencyByCagrOverAvgDrawdownAndExposure = average(
				this::getEfficiencyByCagrOverAvgDrawdownAndExposure, 
				step.getEfficiencyByCagrOverAvgDrawdownAndExposure());
	}
	
	private double average(Supplier<Double> supplier, double value) {
		return (supplier.get() * (steps.size() - 1) + value) / steps.size();
	}
	
	@Override
	public String toString() {
		final String newLine = System.lineSeparator();
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%-50s : %.3f", "Efficiency by CAGR", efficiencyByCagr)).append(newLine);
		builder.append(String.format("%-50s : %.3f", "Efficiency by CAGR over Avg Drawdown", efficiencyByCagrOverAvgDrawdown)).append(newLine);
		builder.append(String.format("%-50s : %.3f", "Efficiency by CAGR over Avg Drawdown and Exposure", 
				efficiencyByCagrOverAvgDrawdownAndExposure)).append(newLine);
		builder.append(performance.toString());
		return builder.toString();
	}

}
