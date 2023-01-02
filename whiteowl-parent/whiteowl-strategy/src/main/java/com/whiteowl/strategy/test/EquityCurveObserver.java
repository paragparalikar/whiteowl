package com.whiteowl.strategy.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class EquityCurveObserver {

	private final Scrip scrip;
	private final Portfolio portfolio;
	private final PositionService positionService;
	private final List<Double> equityCurve = new ArrayList<>();
	
	public void next(final double closePrice) {
		if(equityCurve.isEmpty()) equityCurve.add(portfolio.getAvailableMargin());
		final double value = positionService.findByPortfolioAndStatusNot(portfolio, PositionStatus.CLOSED).stream()
			.map(position -> position.getOnBalanceQuantity(scrip))
			.map(onBalanceQuantity -> onBalanceQuantity * closePrice)
			.collect(Collectors.summingDouble(Double::doubleValue));
		equityCurve.add(value + portfolio.getAvailableMargin());
	}
	
	public List<Double> getEquityCurve(){
		return Collections.unmodifiableList(equityCurve);
	}
	
}
