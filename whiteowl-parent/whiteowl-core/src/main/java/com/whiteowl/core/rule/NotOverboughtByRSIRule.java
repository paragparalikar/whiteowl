package com.whiteowl.core.rule;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.UnderIndicatorRule;

public class NotOverboughtByRSIRule extends AbstractRule {
	
	private final Rule delegate;
	
	public NotOverboughtByRSIRule(
			final int overboughtFilterRSILength,
			final double oberboughtFilterRSIMax,
			final BarSeries barSeries) {
		final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
		final RSIIndicator rsiIndicator = new RSIIndicator(closePriceIndicator, overboughtFilterRSILength);
		this.delegate = new UnderIndicatorRule(rsiIndicator, DoubleNum.valueOf(oberboughtFilterRSIMax));
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		return delegate.isSatisfied(index);
	}

}
