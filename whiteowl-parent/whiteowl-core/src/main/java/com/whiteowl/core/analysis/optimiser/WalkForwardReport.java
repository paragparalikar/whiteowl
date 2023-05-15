package com.whiteowl.core.analysis.optimiser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.Getter;

@Getter
public class WalkForwardReport implements Consumer<WalkForwardStep>, Serializable {
	private static final long serialVersionUID = -3090614699412868090L;

	private final List<WalkForwardStep> steps = new ArrayList<>();
	private double efficiencyByCagr, efficiencyByCagrOverAvgDrawdown, efficiencyByCagrOverAvgDrawdownAndExposure;
			
	@Override
	public void accept(WalkForwardStep step) {
		steps.add(step);
		efficiencyByCagr = average(this::getEfficiencyByCagr, step.getEfficiencyByCagr());
		efficiencyByCagrOverAvgDrawdown = average(this::getEfficiencyByCagrOverAvgDrawdown, step.getEfficiencyByCagrOverAvgDrawdown());
		efficiencyByCagrOverAvgDrawdownAndExposure = average(
				this::getEfficiencyByCagrOverAvgDrawdownAndExposure, 
				step.getEfficiencyByCagrOverAvgDrawdownAndExposure());
	}
	
	private double average(Supplier<Double> supplier, double value) {
		return (supplier.get() * (steps.size() - 1) + value) / steps.size();
	}

}
